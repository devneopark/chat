#!/usr/bin/env python3
"""Plan, validate, build, and publish backend release modules."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


SEMVER_RE = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")
SNAPSHOT_SUFFIX = "-SNAPSHOT"
ROOT_AFFECTS_ALL = {
    "build.gradle.kts",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
}
ROOT_AFFECTING_PREFIXES = ("gradle/wrapper/",)
IGNORED_PREFIXES = ("README.md", "docs/", "tools/release/")


@dataclass(frozen=True)
class Dependency:
    path: str
    artifact_id: str
    requested_version: str
    effective_version: str
    registered: bool


@dataclass(frozen=True)
class Module:
    path: str
    project_dir: str
    artifact_id: str
    base_version: str
    version: str
    type: str
    tag_namespace: str
    dependencies: tuple[Dependency, ...]
    external_dependencies: tuple[str, ...]

    @property
    def snapshot_version(self) -> str:
        return f"{self.base_version}{SNAPSHOT_SUFFIX}"

    @property
    def snapshot_tag(self) -> str:
        return f"{self.tag_namespace}/{self.artifact_id}/v{self.snapshot_version}"

    @property
    def stable_tag_prefix(self) -> str:
        return f"{self.tag_namespace}/{self.artifact_id}/v"


@dataclass(frozen=True)
class Graph:
    group: str
    build_plugins: tuple[tuple[str, str], ...]
    modules: tuple[Module, ...]


def run(
    command: list[str],
    *,
    check: bool = True,
    cwd: Path | None = None,
    capture: bool = True,
) -> subprocess.CompletedProcess[str]:
    print("+ " + " ".join(command), flush=True)
    return subprocess.run(
        command,
        check=check,
        cwd=cwd,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
    )


def git_output(args: list[str], *, check: bool = True) -> str:
    result = run(["git", *args], check=check)
    return result.stdout.strip()


def github_annotation(level: str, title: str, message: str) -> None:
    def escape(value: str) -> str:
        return (
            value.replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")
            .replace(":", "%3A")
            .replace(",", "%2C")
        )

    print(f"::{level} title={escape(title)}::{escape(message)}")


def write_summary(lines: Iterable[str]) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with Path(summary_path).open("a") as summary:
        summary.write("\n".join(lines) + "\n")


def write_github_output(values: dict[str, str]) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    if not output_path:
        return
    with Path(output_path).open("a") as output:
        for key, value in values.items():
            output.write(f"{key}={value}\n")


def parse_base_version(version: str) -> str:
    base = version.removesuffix(SNAPSHOT_SUFFIX)
    if not SEMVER_RE.fullmatch(base):
        raise SystemExit(f"Unsupported backend module version: {version}")
    return base


def version_key(version: str) -> tuple[int, int, int]:
    match = SEMVER_RE.fullmatch(version)
    if not match:
        raise SystemExit(f"Unsupported stable version: {version}")
    return tuple(int(part) for part in match.groups())  # type: ignore[return-value]


def default_tag_namespace(module_type: str) -> str:
    return "backend/libs" if module_type == "library" else "backend/services"


def load_graph(path: Path) -> Graph:
    data = json.loads(path.read_text())
    modules: list[Module] = []
    for item in data.get("modules", []):
        module_type = item["type"]
        version = item["version"]
        dependencies = tuple(
            Dependency(
                path=dependency.get("path", ""),
                artifact_id=dependency["artifactId"],
                requested_version=dependency.get("requestedVersion", ""),
                effective_version=dependency.get("effectiveVersion", ""),
                registered=dependency.get("registered", True),
            )
            for dependency in item.get("dependencies", [])
        )
        modules.append(
            Module(
                path=item["path"],
                project_dir=item["projectDir"].rstrip("/"),
                artifact_id=item["artifactId"],
                base_version=item.get("baseVersion", parse_base_version(version)),
                version=version,
                type=module_type,
                tag_namespace=item.get("tagNamespace", default_tag_namespace(module_type)),
                dependencies=dependencies,
                external_dependencies=tuple(sorted(item.get("externalDependencies", []))),
            )
        )
    return Graph(
        group=data.get("group", ""),
        build_plugins=tuple(sorted(data.get("buildPlugins", {}).items())),
        modules=tuple(sorted(modules, key=lambda module: module.path)),
    )


def module_payload(module: Module) -> dict[str, Any]:
    return {
        "path": module.path,
        "projectDir": module.project_dir,
        "artifactId": module.artifact_id,
        "baseVersion": module.base_version,
        "version": module.version,
        "type": module.type,
        "tagNamespace": module.tag_namespace,
        "dependencies": [
            {
                "path": dependency.path,
                "artifactId": dependency.artifact_id,
                "requestedVersion": dependency.requested_version,
                "effectiveVersion": dependency.effective_version,
                "registered": dependency.registered,
            }
            for dependency in module.dependencies
        ],
        "externalDependencies": list(module.external_dependencies),
    }


def module_build_inputs(module: Module) -> tuple[Any, ...]:
    return (
        module.project_dir,
        module.artifact_id,
        module.base_version,
        module.type,
        tuple(
            sorted(
                (
                    dependency.path,
                    dependency.artifact_id,
                    dependency.requested_version,
                    dependency.registered,
                )
                for dependency in module.dependencies
            )
        ),
        module.external_dependencies,
    )


def current_repo_prefix() -> str:
    git_root = Path(git_output(["rev-parse", "--show-toplevel"])).resolve()
    cwd = Path.cwd().resolve()
    try:
        relative = cwd.relative_to(git_root)
    except ValueError:
        return ""
    return "" if str(relative) == "." else relative.as_posix().rstrip("/") + "/"


def changed_files(base: str, head: str, repo_prefix: str) -> list[str]:
    output = git_output(["diff", "--name-only", f"{base}..{head}"])
    files: list[str] = []
    for line in output.splitlines():
        normalized = line.strip().replace("\\", "/")
        if not normalized:
            continue
        if repo_prefix:
            if not normalized.startswith(repo_prefix):
                continue
            normalized = normalized[len(repo_prefix) :]
        files.append(normalized)
    return files


def find_containing_module(file_path: str, modules: Iterable[Module]) -> Module | None:
    for module in sorted(modules, key=lambda item: len(item.project_dir), reverse=True):
        if file_path == module.project_dir or file_path.startswith(module.project_dir + "/"):
            return module
    return None


def is_module_build_file(file_path: str, module: Module) -> bool:
    return file_path == f"{module.project_dir}/build.gradle.kts"


def validate_single_pr_module(base_graph: Graph, head_graph: Graph, files: list[str]) -> None:
    """Allow one backend module content change per PR.

    Existing module build.gradle.kts dependency changes are intentionally
    excluded from the file count so a PR can adjust downstream dependency
    declarations while changing the implementation of exactly one module.
    Module version changes still count as module changes.
    """

    base_by_artifact = {module.artifact_id: module for module in base_graph.modules}
    head_by_artifact = {module.artifact_id: module for module in head_graph.modules}
    added_artifacts = set(head_by_artifact) - set(base_by_artifact)
    deleted_artifacts = set(base_by_artifact) - set(head_by_artifact)
    changed: dict[str, set[str]] = {}

    def mark(module: Module, reason: str) -> None:
        changed.setdefault(module.path, set()).add(reason)

    for artifact_id in sorted(added_artifacts):
        mark(head_by_artifact[artifact_id], "module-added")
    for artifact_id in sorted(deleted_artifacts):
        mark(base_by_artifact[artifact_id], "module-deleted")
    for artifact_id in sorted(set(base_by_artifact) & set(head_by_artifact)):
        base_module = base_by_artifact[artifact_id]
        head_module = head_by_artifact[artifact_id]
        if base_module.base_version != head_module.base_version:
            mark(head_module, "module-version-changed")

    for file_path in files:
        head_module = find_containing_module(file_path, head_graph.modules)
        base_module = find_containing_module(file_path, base_graph.modules)
        module = head_module or base_module
        if not module:
            continue

        is_existing_module = module.artifact_id in base_by_artifact and module.artifact_id in head_by_artifact
        if (
            is_existing_module
            and (
                (head_module and is_module_build_file(file_path, head_module))
                or (base_module and is_module_build_file(file_path, base_module))
            )
        ):
            continue

        mark(module, file_path)

    if len(changed) <= 1:
        return

    lines = [
        "A backend PR may change source/content files in only one backend module.",
        "Existing module build.gradle.kts dependency changes are allowed; module version changes still count.",
        "",
        "Changed backend modules:",
    ]
    for module_path, reasons in sorted(changed.items()):
        lines.append(f"- {module_path}: {', '.join(sorted(reasons))}")
    message = "\n".join(lines)
    github_annotation("error", "Multiple backend modules changed", message)
    raise SystemExit(message)


def reverse_closure(direct: set[str], modules: Iterable[Module]) -> set[str]:
    reverse: dict[str, set[str]] = {}
    for module in modules:
        reverse.setdefault(module.path, set())
        for dependency in module.dependencies:
            if dependency.registered and dependency.path:
                reverse.setdefault(dependency.path, set()).add(module.path)

    affected = set(direct)
    queue = sorted(direct)
    while queue:
        current = queue.pop(0)
        for consumer in sorted(reverse.get(current, set())):
            if consumer not in affected:
                affected.add(consumer)
                queue.append(consumer)
    return affected


def dependency_layers(affected: set[str], modules: Iterable[Module]) -> list[list[str]]:
    by_path = {module.path: module for module in modules}
    remaining = set(affected)
    layers: list[list[str]] = []
    completed: set[str] = set()

    while remaining:
        ready = sorted(
            path
            for path in remaining
            if all(
                dependency.path not in affected or dependency.path in completed
                for dependency in by_path[path].dependencies
                if dependency.registered
            )
        )
        if not ready:
            cycle = ", ".join(sorted(remaining))
            raise SystemExit(f"Dependency cycle detected among affected modules: {cycle}")
        layers.append(ready)
        completed.update(ready)
        remaining.difference_update(ready)
    return layers


def latest_stable_tag(module: Module) -> tuple[str, str] | None:
    output = git_output(["tag", "--list", f"{module.stable_tag_prefix}*"], check=False)
    candidates: list[tuple[tuple[int, int, int], str, str]] = []
    for tag in output.splitlines():
        version = tag.removeprefix(module.stable_tag_prefix)
        if version.endswith(SNAPSHOT_SUFFIX) or not SEMVER_RE.fullmatch(version):
            continue
        candidates.append((version_key(version), version, tag))
    if not candidates:
        return None
    _, version, tag = sorted(candidates)[-1]
    return version, tag


def validate_registered_dependencies(modules: Iterable[Module]) -> list[str]:
    errors: list[str] = []
    for module in modules:
        for dependency in module.dependencies:
            if not dependency.registered or not dependency.path:
                errors.append(
                    f"{module.artifact_id} references unregistered backend artifact "
                    f"{dependency.artifact_id}"
                )
    return errors


def validate_versions(modules: Iterable[Module], *, resumable_sha: str | None = None) -> list[str]:
    errors: list[str] = []
    summary_rows = [
        "## Backend stable version validation",
        "",
        "| Module | Declared | Latest stable | Result |",
        "|---|---:|---:|---|",
    ]
    for module in sorted(modules, key=lambda item: item.artifact_id):
        latest = latest_stable_tag(module)
        if latest is None:
            summary_rows.append(f"| `{module.artifact_id}` | `{module.base_version}` | none | pass |")
            continue
        latest_version, latest_tag = latest
        if version_key(module.base_version) <= version_key(latest_version):
            tag_sha = (
                git_output(["rev-list", "-n", "1", latest_tag], check=False)
                if resumable_sha
                else ""
            )
            if (
                resumable_sha
                and module.base_version == latest_version
                and tag_sha == resumable_sha
            ):
                summary_rows.append(
                    f"| `{module.artifact_id}` | `{module.base_version}` | `{latest_version}` | resume |"
                )
                continue
            message = (
                f"{module.artifact_id}: declared version {module.base_version} must be greater than "
                f"published stable {latest_version} ({latest_tag}). "
                f"Increase version in {module.project_dir}/build.gradle.kts."
            )
            errors.append(message)
            github_annotation("error", "Backend stable version collision", message)
            summary_rows.append(
                f"| `{module.artifact_id}` | `{module.base_version}` | `{latest_version}` | **fail** |"
            )
        else:
            summary_rows.append(
                f"| `{module.artifact_id}` | `{module.base_version}` | `{latest_version}` | pass |"
            )
    write_summary(summary_rows + [""])
    return errors


def build_plan(
    base_graph: Graph,
    head_graph: Graph,
    files: list[str],
    *,
    resumable_sha: str | None = None,
    enforce_single_pr_module: bool = False,
) -> dict[str, Any]:
    base_by_artifact = {module.artifact_id: module for module in base_graph.modules}
    head_by_artifact = {module.artifact_id: module for module in head_graph.modules}
    head_by_path = {module.path: module for module in head_graph.modules}
    added_artifacts = set(head_by_artifact) - set(base_by_artifact)
    deleted_artifacts = set(base_by_artifact) - set(head_by_artifact)

    if enforce_single_pr_module:
        validate_single_pr_module(base_graph, head_graph, files)

    direct = {head_by_artifact[artifact].path for artifact in added_artifacts}
    reasons: dict[str, set[str]] = {path: {"module-added"} for path in direct}

    def mark(path: str, reason: str) -> None:
        direct.add(path)
        reasons.setdefault(path, set()).add(reason)

    common_artifacts = set(base_by_artifact) & set(head_by_artifact)
    for artifact_id in common_artifacts:
        base_module = base_by_artifact[artifact_id]
        head_module = head_by_artifact[artifact_id]
        if module_build_inputs(base_module) != module_build_inputs(head_module):
            mark(head_module.path, "effective-build-input-changed")

    if base_graph.build_plugins != head_graph.build_plugins:
        for path in head_by_path:
            mark(path, "common-build-plugin-changed")

    affects_all = False
    for file_path in files:
        if file_path in ROOT_AFFECTS_ALL or file_path.startswith(ROOT_AFFECTING_PREFIXES):
            affects_all = True
            continue
        if file_path == "settings.gradle.kts" or file_path == "gradle/libs.versions.toml":
            continue
        if file_path.startswith(IGNORED_PREFIXES):
            continue
        module = find_containing_module(file_path, head_graph.modules)
        if module:
            mark(module.path, f"file-changed:{file_path}")

    if affects_all:
        for path in head_by_path:
            mark(path, "common-gradle-input-changed")

    affected = reverse_closure(direct, head_graph.modules)
    layers = dependency_layers(affected, head_graph.modules)
    registered_errors = validate_registered_dependencies(head_graph.modules)
    if registered_errors:
        for error in registered_errors:
            github_annotation("error", "Unregistered backend dependency", error)
        raise SystemExit("\n".join(registered_errors))

    affected_modules = [head_by_path[path] for path in sorted(affected)]
    version_errors = validate_versions(affected_modules, resumable_sha=resumable_sha)
    if version_errors:
        raise SystemExit("\n".join(version_errors))

    plan = {
        "changedFiles": sorted(files),
        "direct": [module_payload(head_by_path[path]) for path in sorted(direct)],
        "affected": [module_payload(head_by_path[path]) for path in sorted(affected)],
        "deleted": [module_payload(base_by_artifact[artifact]) for artifact in sorted(deleted_artifacts)],
        "layers": [
            [module_payload(head_by_path[path]) for path in layer]
            for layer in layers
        ],
        "reasons": {path: sorted(values) for path, values in sorted(reasons.items())},
    }
    return plan


def render_plan_summary(plan: dict[str, Any]) -> None:
    lines = [
        "## Backend release plan",
        "",
        f"- Direct modules: {len(plan['direct'])}",
        f"- Affected modules: {len(plan['affected'])}",
        f"- Deleted modules: {len(plan['deleted'])}",
        f"- Dependency layers: {len(plan['layers'])}",
        "",
    ]
    for index, layer in enumerate(plan["layers"]):
        artifacts = ", ".join(f"`{module['artifactId']}`" for module in layer)
        lines.append(f"- Layer {index}: {artifacts}")
    if plan["deleted"]:
        deleted = ", ".join(f"`{module['artifactId']}`" for module in plan["deleted"])
        lines.extend(["", f"- Deleted: {deleted}"])
    lines.append("")
    write_summary(lines)


def plan_command(args: argparse.Namespace) -> None:
    base_graph = load_graph(Path(args.base_graph))
    head_graph = load_graph(Path(args.head_graph))
    files = changed_files(args.base, args.head, current_repo_prefix())
    plan = build_plan(
        base_graph,
        head_graph,
        files,
        resumable_sha=args.allow_stable_at_head,
        enforce_single_pr_module=args.enforce_single_pr_module,
    )
    output = json.dumps(plan, ensure_ascii=False, indent=2) + "\n"
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.output).write_text(output)
    render_plan_summary(plan)
    write_github_output(
        {
            "has_affected": "true" if plan["affected"] else "false",
            "has_deleted": "true" if plan["deleted"] else "false",
            "has_work": "true" if plan["affected"] or plan["deleted"] else "false",
            "affected_count": str(len(plan["affected"])),
            "layer_count": str(len(plan["layers"])),
        }
    )
    print(output, end="")


def load_plan(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text())


def gradle_layer(
    gradlew: str,
    modules: list[dict[str, Any]],
    action: str,
) -> None:
    tasks: list[str] = []
    for module in modules:
        if action == "build":
            task = "build"
        elif module["type"] == "library":
            task = "publish"
        else:
            task = "bootJar"
        tasks.append(f"{module['path']}:{task}")
    if not tasks:
        return
    command = [gradlew, "--parallel", "-PreleaseChannel=snapshot"]
    command.extend(tasks)
    run(command, capture=False)


def build_command(args: argparse.Namespace) -> None:
    plan = load_plan(Path(args.plan))
    if not plan["affected"]:
        print("No affected backend modules; build skipped.")
        return
    for index, layer in enumerate(plan["layers"]):
        artifacts = ", ".join(module["artifactId"] for module in layer)
        print(f"Building layer {index}: {artifacts}")
        gradle_layer(args.gradlew, layer, "build")


def git_tag_snapshot(module: dict[str, Any], sha: str) -> str:
    tag = f"{module['tagNamespace']}/{module['artifactId']}/v{module['version']}"
    run(["git", "tag", "-f", tag, sha], capture=False)
    run(["git", "push", "--force", "origin", f"refs/tags/{tag}"], capture=False)
    return tag


def publish_service_release(module: dict[str, Any], tag: str, sha: str) -> None:
    jar = Path(module["projectDir"]) / "build" / "libs" / (
        f"backend-{module['artifactId']}-{module['version']}.jar"
    )
    if not jar.is_file():
        raise SystemExit(f"Service bootJar not found: {jar}")
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    title = f"backend/services/{module['artifactId']} {module['version']}"
    notes = Path("build") / f"release-notes-{module['artifactId']}.md"
    notes.parent.mkdir(parents=True, exist_ok=True)
    notes.write_text(
        "\n".join(
            [
                "## Backend SNAPSHOT",
                "",
                f"- Module: `{module['artifactId']}`",
                f"- Version: `{module['version']}`",
                f"- Commit: `{sha}`",
                f"- Asset SHA-256: `{digest}`",
                f"- Workflow: `{os.environ.get('GITHUB_SERVER_URL', '')}/{os.environ.get('GITHUB_REPOSITORY', '')}/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}`",
                "",
            ]
        )
    )
    existing = run(["gh", "release", "view", tag], check=False)
    if existing.returncode == 0:
        run(
            [
                "gh",
                "release",
                "edit",
                tag,
                "--prerelease",
                "--title",
                title,
                "--notes-file",
                str(notes),
            ],
            capture=False,
        )
        run(["gh", "release", "upload", tag, str(jar), "--clobber"], capture=False)
    else:
        run(
            [
                "gh",
                "release",
                "create",
                tag,
                str(jar),
                "--verify-tag",
                "--prerelease",
                "--title",
                title,
                "--notes-file",
                str(notes),
            ],
            capture=False,
        )


def github_api_json(url: str, token: str) -> Any:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read().decode())


def github_api_delete(url: str, token: str) -> None:
    request = urllib.request.Request(
        url,
        method="DELETE",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(request):
            return
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise


def delete_maven_snapshot(module: dict[str, Any]) -> None:
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    owner = os.environ.get("GITHUB_REPOSITORY_OWNER")
    if not token or not owner:
        raise SystemExit("GITHUB_TOKEN and GITHUB_REPOSITORY_OWNER are required for Package cleanup")
    package_names = [
        f"com.devneopark.chat.backend:{module['artifactId']}",
        f"com.devneopark.chat.backend.{module['artifactId']}",
    ]
    target_version = f"{module['baseVersion']}{SNAPSHOT_SUFFIX}"
    for package_name in package_names:
        package = urllib.parse.quote(package_name, safe="")
        for owner_kind in ("users", "orgs"):
            base_url = f"https://api.github.com/{owner_kind}/{owner}/packages/maven/{package}/versions"
            try:
                versions = github_api_json(f"{base_url}?per_page=100", token)
            except urllib.error.HTTPError as exc:
                if exc.code == 404:
                    continue
                raise
            for version in versions:
                if version.get("name") == target_version:
                    github_api_delete(f"{base_url}/{version['id']}", token)
                    print(f"Deleted Maven snapshot {package_name}:{target_version}")
                    return
            return
    print(f"Maven snapshot already absent: {module['artifactId']}:{target_version}")


def delete_snapshot_tag(tag: str) -> None:
    result = run(["git", "ls-remote", "--exit-code", "--tags", "origin", f"refs/tags/{tag}"], check=False)
    if result.returncode == 0:
        run(["git", "push", "--delete", "origin", tag], capture=False)


def cleanup_deleted_snapshot(module: dict[str, Any]) -> None:
    version = f"{module['baseVersion']}{SNAPSHOT_SUFFIX}"
    namespace = module.get("tagNamespace") or default_tag_namespace(module["type"])
    tag = f"{namespace}/{module['artifactId']}/v{version}"
    if module["type"] == "service":
        run(["gh", "release", "delete", tag, "--yes", "--cleanup-tag"], check=False, capture=False)
    else:
        delete_maven_snapshot(module)
    delete_snapshot_tag(tag)


def stable_tag(module: dict[str, Any]) -> str:
    return f"{module['tagNamespace']}/{module['artifactId']}/v{module['baseVersion']}"


def remote_tag_sha(tag: str) -> str | None:
    result = run(
        ["git", "ls-remote", "--tags", "origin", f"refs/tags/{tag}"],
        check=False,
    )
    if result.returncode != 0 or not result.stdout.strip():
        return None
    return result.stdout.split()[0]


def preflight_stable_tags(modules: Iterable[dict[str, Any]], sha: str) -> None:
    errors: list[str] = []
    for module in modules:
        tag = stable_tag(module)
        existing_sha = remote_tag_sha(tag)
        if existing_sha and existing_sha != sha:
            errors.append(f"{tag} already points to {existing_sha}, expected {sha}")
    if errors:
        for error in errors:
            github_annotation("error", "Stable tag conflict", error)
        raise SystemExit("\n".join(errors))


def maven_pom_commit(module: dict[str, Any]) -> str | None:
    username = os.environ.get("GH_PACKAGES_USERNAME") or os.environ.get("GITHUB_ACTOR")
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    repository = os.environ.get("GITHUB_REPOSITORY", "devneopark/chat")
    if not username or not token:
        raise SystemExit("GH_PACKAGES_USERNAME and GITHUB_TOKEN are required for stable Package checks")
    group_path = "com/devneopark/chat/backend"
    artifact_id = module["artifactId"]
    version = module["baseVersion"]
    pom_url = (
        f"https://maven.pkg.github.com/{repository}/{group_path}/{artifact_id}/"
        f"{version}/{artifact_id}-{version}.pom"
    )
    credentials = base64.b64encode(f"{username}:{token}".encode()).decode()
    request = urllib.request.Request(
        pom_url,
        headers={"Authorization": f"Basic {credentials}"},
    )
    try:
        with urllib.request.urlopen(request) as response:
            root = ElementTree.fromstring(response.read())
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return None
        raise
    commit = root.findtext(".//{*}properties/{*}release.commit")
    if not commit:
        raise SystemExit(
            f"Stable Package exists without release.commit provenance: {artifact_id}:{version}"
        )
    return commit.strip()


def ensure_stable_tag(module: dict[str, Any], sha: str) -> str:
    tag = stable_tag(module)
    existing_sha = remote_tag_sha(tag)
    if existing_sha is None:
        run(["git", "tag", tag, sha], capture=False)
        run(["git", "push", "origin", f"refs/tags/{tag}"], capture=False)
    elif existing_sha != sha:
        raise SystemExit(f"Stable tag conflict: {tag} points to {existing_sha}, expected {sha}")
    return tag


def stable_release_exists(tag: str) -> bool:
    return run(["gh", "release", "view", tag], check=False).returncode == 0


def create_stable_service_release(module: dict[str, Any], tag: str, sha: str) -> None:
    if stable_release_exists(tag):
        print(f"Stable service Release already exists: {tag}")
        return
    jar = Path(module["projectDir"]) / "build" / "libs" / (
        f"backend-{module['artifactId']}-{module['baseVersion']}.jar"
    )
    if not jar.is_file():
        raise SystemExit(f"Stable service bootJar not found: {jar}")
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    title = f"backend/services/{module['artifactId']} {module['baseVersion']}"
    notes = Path("build") / f"stable-release-notes-{module['artifactId']}.md"
    notes.parent.mkdir(parents=True, exist_ok=True)
    notes.write_text(
        "\n".join(
            [
                "## Backend stable release",
                "",
                f"- Module: `{module['artifactId']}`",
                f"- Version: `{module['baseVersion']}`",
                f"- Commit: `{sha}`",
                f"- Asset SHA-256: `{digest}`",
                "",
            ]
        )
    )
    run(
        [
            "gh",
            "release",
            "create",
            tag,
            str(jar),
            "--verify-tag",
            "--title",
            title,
            "--notes-file",
            str(notes),
        ],
        capture=False,
    )


def publish_stable_command(args: argparse.Namespace) -> None:
    plan = load_plan(Path(args.plan))
    sha = os.environ.get("GITHUB_SHA") or git_output(["rev-parse", "HEAD"])
    preflight_stable_tags(plan["affected"], sha)
    publish_rows = [
        "## Backend stable publish",
        "",
        "| Module | Version | Result |",
        "|---|---:|---|",
    ]

    for index, layer in enumerate(plan["layers"]):
        pending: list[dict[str, Any]] = []
        for module in layer:
            tag = stable_tag(module)
            if module["type"] == "library":
                package_commit = maven_pom_commit(module)
                if package_commit is None:
                    pending.append(module)
                elif package_commit == sha:
                    ensure_stable_tag(module, sha)
                    publish_rows.append(
                        f"| `{module['artifactId']}` | `{module['baseVersion']}` | resumed |"
                    )
                else:
                    raise SystemExit(
                        f"Stable Package conflict: {module['artifactId']}:{module['baseVersion']} "
                        f"was published from {package_commit}, expected {sha}"
                    )
            else:
                existing_sha = remote_tag_sha(tag)
                if existing_sha == sha and stable_release_exists(tag):
                    publish_rows.append(
                        f"| `{module['artifactId']}` | `{module['baseVersion']}` | resumed |"
                    )
                else:
                    pending.append(module)

        if pending:
            artifacts = ", ".join(module["artifactId"] for module in pending)
            print(f"Publishing stable layer {index}: {artifacts}")
            tasks = [
                f"{module['path']}:{'publish' if module['type'] == 'library' else 'bootJar'}"
                for module in pending
            ]
            run(
                [args.gradlew, "--parallel", "-PreleaseChannel=stable", *tasks],
                capture=False,
            )
            for module in pending:
                tag = ensure_stable_tag(module, sha)
                if module["type"] == "service":
                    create_stable_service_release(module, tag, sha)
                publish_rows.append(
                    f"| `{module['artifactId']}` | `{module['baseVersion']}` | published |"
                )

    for module in [*plan["affected"], *plan["deleted"]]:
        cleanup_deleted_snapshot(module)

    if not plan["affected"] and not plan["deleted"]:
        print("No affected or deleted backend modules; stable publish skipped.")
    write_summary(publish_rows + [""])


def publish_snapshot_command(args: argparse.Namespace) -> None:
    plan = load_plan(Path(args.plan))
    sha = os.environ.get("GITHUB_SHA") or git_output(["rev-parse", "HEAD"])
    publish_rows = [
        "## Backend SNAPSHOT publish",
        "",
        "| Module | Version | Result |",
        "|---|---:|---|",
    ]
    for index, layer in enumerate(plan["layers"]):
        artifacts = ", ".join(module["artifactId"] for module in layer)
        print(f"Publishing layer {index}: {artifacts}")
        gradle_layer(args.gradlew, layer, "publish")
        for module in layer:
            tag = git_tag_snapshot(module, sha)
            if module["type"] == "service":
                publish_service_release(module, tag, sha)
            publish_rows.append(
                f"| `{module['artifactId']}` | `{module['version']}` | published |"
            )
    for module in plan["deleted"]:
        cleanup_deleted_snapshot(module)
        publish_rows.append(
            f"| `{module['artifactId']}` | `{module['baseVersion']}-SNAPSHOT` | deleted |"
        )
    if not plan["affected"] and not plan["deleted"]:
        print("No affected or deleted backend modules; publish skipped.")
    write_summary(publish_rows + [""])


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    plan_parser = subparsers.add_parser("plan")
    plan_parser.add_argument("--base-graph", required=True)
    plan_parser.add_argument("--head-graph", required=True)
    plan_parser.add_argument("--base", required=True)
    plan_parser.add_argument("--head", required=True)
    plan_parser.add_argument("--output", required=True)
    plan_parser.add_argument("--allow-stable-at-head")
    plan_parser.add_argument("--enforce-single-pr-module", action="store_true")
    plan_parser.set_defaults(func=plan_command)

    build_parser = subparsers.add_parser("build")
    build_parser.add_argument("--plan", required=True)
    build_parser.add_argument("--gradlew", default="./gradlew")
    build_parser.set_defaults(func=build_command)

    publish_parser = subparsers.add_parser("publish-snapshot")
    publish_parser.add_argument("--plan", required=True)
    publish_parser.add_argument("--gradlew", default="./gradlew")
    publish_parser.set_defaults(func=publish_snapshot_command)

    stable_parser = subparsers.add_parser("publish-stable")
    stable_parser.add_argument("--plan", required=True)
    stable_parser.add_argument("--gradlew", default="./gradlew")
    stable_parser.set_defaults(func=publish_stable_command)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        if exc.stdout:
            sys.stdout.write(exc.stdout)
        if exc.stderr:
            sys.stderr.write(exc.stderr)
        raise SystemExit(exc.returncode) from exc
