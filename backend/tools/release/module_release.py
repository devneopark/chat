#!/usr/bin/env python3
"""Release workflow helper for backend modules."""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SEMVER_RE = re.compile(r"^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)(?P<snapshot>-SNAPSHOT)?$")
RELEASE_LABELS = {"release:patch", "release:minor", "release:major"}


@dataclass(frozen=True)
class Module:
    path: str
    project_dir: str
    artifact_id: str
    version_alias: str
    version: str
    type: str
    dependencies: tuple[str, ...]


def run(command: list[str], *, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, check=check, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def git_output(args: list[str]) -> str:
    return run(["git", *args]).stdout.strip()


def current_repo_prefix() -> str:
    git_root = Path(git_output(["rev-parse", "--show-toplevel"])).resolve()
    cwd = Path.cwd().resolve()
    try:
        relative = cwd.relative_to(git_root)
    except ValueError:
        return ""
    if str(relative) == ".":
        return ""
    return relative.as_posix().rstrip("/") + "/"


def normalize_git_path(path: str, repo_prefix: str) -> str | None:
    normalized = path.strip().replace("\\", "/")
    if not normalized:
        return None
    if repo_prefix and normalized.startswith(repo_prefix):
        return normalized[len(repo_prefix) :]
    if repo_prefix:
        return None
    return normalized


def load_graph(path: Path) -> tuple[str, list[Module]]:
    data = json.loads(path.read_text())
    modules = []
    for item in data["modules"]:
        modules.append(
            Module(
                path=item["path"],
                project_dir=item["projectDir"].rstrip("/"),
                artifact_id=item["artifactId"],
                version_alias=item["versionAlias"],
                version=item["version"],
                type=item["type"],
                dependencies=tuple(dependency["path"] for dependency in item.get("dependencies", [])),
            )
        )
    return data["group"], modules


def module_maps(modules: list[Module]) -> tuple[dict[str, Module], dict[str, Module]]:
    by_path = {module.path: module for module in modules}
    by_artifact = {module.artifact_id: module for module in modules}
    return by_path, by_artifact


def changed_files(base: str, head: str, repo_prefix: str) -> list[str]:
    output = git_output(["diff", "--name-only", f"{base}..{head}"])
    result: list[str] = []
    for line in output.splitlines():
        normalized = normalize_git_path(line, repo_prefix)
        if normalized is not None:
            result.append(normalized)
    return result


def latest_stable_tag(artifact_id: str) -> str | None:
    output = run(["git", "tag", "--list", f"{artifact_id}-v[0-9]*"], check=False).stdout
    candidates: list[tuple[tuple[int, int, int], str]] = []
    for tag in output.splitlines():
        version = tag.removeprefix(f"{artifact_id}-v")
        match = SEMVER_RE.match(version)
        if not match or match.group("snapshot"):
            continue
        candidates.append(((int(match.group("major")), int(match.group("minor")), int(match.group("patch"))), tag))
    if not candidates:
        return None
    return sorted(candidates)[-1][1]


def classify_changed_files(files: list[str], modules: list[Module]) -> tuple[set[str], bool]:
    root_affects_all = {
        "build.gradle.kts",
        "settings.gradle.kts",
        "gradle/libs.versions.toml",
        "gradlew",
        "gradlew.bat",
    }
    root_affecting_prefixes = ("gradle/wrapper/",)
    docs_prefixes = ("README.md", "docs/")
    workflow_prefixes = (".github/workflows/", "tools/release/")

    direct: set[str] = set()
    all_modules = False
    sorted_modules = sorted(modules, key=lambda module: len(module.project_dir), reverse=True)

    for file_path in files:
        if file_path in root_affects_all or file_path.startswith(root_affecting_prefixes):
            all_modules = True
            continue
        if file_path.startswith(docs_prefixes) or file_path.startswith(workflow_prefixes):
            continue
        for module in sorted_modules:
            if file_path == module.project_dir or file_path.startswith(module.project_dir + "/"):
                direct.add(module.path)
                break
    return direct, all_modules


def reverse_closure(direct: set[str], modules: list[Module]) -> set[str]:
    reverse: dict[str, set[str]] = {module.path: set() for module in modules}
    for module in modules:
        for dependency in module.dependencies:
            reverse.setdefault(dependency, set()).add(module.path)

    affected = set(direct)
    queue = list(direct)
    while queue:
        current = queue.pop(0)
        for consumer in reverse.get(current, set()):
            if consumer not in affected:
                affected.add(consumer)
                queue.append(consumer)
    return affected


def topo_sort(affected: set[str], modules: list[Module]) -> list[str]:
    by_path, _ = module_maps(modules)
    visiting: set[str] = set()
    visited: set[str] = set()
    ordered: list[str] = []

    def visit(path: str) -> None:
        if path in visited:
            return
        if path in visiting:
            raise SystemExit(f"Dependency cycle detected around {path}")
        visiting.add(path)
        for dependency in by_path[path].dependencies:
            if dependency in affected:
                visit(dependency)
        visiting.remove(path)
        visited.add(path)
        ordered.append(path)

    for path in sorted(affected):
        visit(path)
    return ordered


def module_payload(modules: list[Module], paths: list[str]) -> list[dict[str, str]]:
    by_path, _ = module_maps(modules)
    return [
        {
            "path": by_path[path].path,
            "projectDir": by_path[path].project_dir,
            "artifactId": by_path[path].artifact_id,
            "versionAlias": by_path[path].version_alias,
            "version": by_path[path].version,
            "type": by_path[path].type,
        }
        for path in paths
    ]


def write_github_output(values: dict[str, str]) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    if not output_path:
        return
    with Path(output_path).open("a") as output:
        for key, value in values.items():
            output.write(f"{key}={value}\n")


def detect_affected(args: argparse.Namespace) -> None:
    _, modules = load_graph(Path(args.graph))
    repo_prefix = current_repo_prefix()

    if args.stable:
        all_direct: set[str] = set()
        all_modules = False
        if args.base:
            direct, root_all = classify_changed_files(changed_files(args.base, args.head, repo_prefix), modules)
            all_direct.update(direct)
            all_modules = all_modules or root_all
        for module in modules:
            tag = latest_stable_tag(module.artifact_id)
            if tag is None:
                all_direct.add(module.path)
                continue
            module_files = changed_files(tag, args.head, repo_prefix)
            direct, root_all = classify_changed_files(module_files, [module])
            if root_all:
                all_modules = True
            if direct:
                all_direct.add(module.path)
        direct_paths = {module.path for module in modules} if all_modules else all_direct
    else:
        if not args.base:
            raise SystemExit("--base is required unless --stable is used")
        files = changed_files(args.base, args.head, repo_prefix)
        direct, all_modules = classify_changed_files(files, modules)
        direct_paths = {module.path for module in modules} if all_modules else direct

    affected = reverse_closure(direct_paths, modules)
    ordered_paths = topo_sort(affected, modules)
    direct_ordered = topo_sort(direct_paths, modules) if direct_paths else []
    payload = {
        "direct": module_payload(modules, direct_ordered),
        "affected": module_payload(modules, ordered_paths),
    }
    output = json.dumps(payload, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(output + "\n")
    else:
        print(output)

    compact_affected = json.dumps(payload["affected"], ensure_ascii=False, separators=(",", ":"))
    write_github_output(
        {
            "has_affected": "true" if ordered_paths else "false",
            "affected_json": compact_affected,
            "affected_paths": " ".join(ordered_paths),
            "affected_artifacts": " ".join(item["artifactId"] for item in payload["affected"]),
        }
    )


def parse_release_level(labels_json: str) -> str:
    try:
        labels = set(json.loads(labels_json))
    except json.JSONDecodeError as exc:
        raise SystemExit(f"Invalid labels JSON: {exc}") from exc
    release_labels = labels & RELEASE_LABELS
    if len(release_labels) > 1:
        raise SystemExit(f"Multiple release labels are not allowed: {sorted(release_labels)}")
    if not release_labels:
        return "patch"
    return next(iter(release_labels)).split(":", 1)[1]


def parse_version(version: str) -> tuple[int, int, int, bool]:
    match = SEMVER_RE.match(version)
    if not match:
        raise SystemExit(f"Unsupported version: {version}")
    return (
        int(match.group("major")),
        int(match.group("minor")),
        int(match.group("patch")),
        bool(match.group("snapshot")),
    )


def bump_version(version: str, level: str) -> str:
    major, minor, patch, snapshot = parse_version(version)
    if level == "major":
        major, minor, patch = major + 1, 0, 0
    elif level == "minor":
        minor, patch = minor + 1, 0
    elif level == "patch":
        if not snapshot:
            patch += 1
    else:
        raise SystemExit(f"Unsupported release level: {level}")
    return f"{major}.{minor}.{patch}-SNAPSHOT"


def version_key(version: str) -> tuple[int, int, int]:
    major, minor, patch, _ = parse_version(version)
    return major, minor, patch


def update_versions(version_file: Path, replacements: dict[str, str]) -> bool:
    original = version_file.read_text()
    updated = original
    for alias, version in replacements.items():
        pattern = re.compile(rf'^({re.escape(alias)}\s*=\s*")[^"]+(".*)$', re.MULTILINE)
        updated, count = pattern.subn(rf'\g<1>{version}\2', updated)
        if count != 1:
            raise SystemExit(f"Version alias not found or duplicated in {version_file}: {alias}")
    if updated != original:
        version_file.write_text(updated)
        return True
    return False


def load_affected(path: Path) -> tuple[list[dict[str, str]], set[str]]:
    data = json.loads(path.read_text())
    affected = data.get("affected", [])
    direct = {item["path"] for item in data.get("direct", [])}
    return affected, direct


def bump_snapshots(args: argparse.Namespace) -> None:
    affected, direct_paths = load_affected(Path(args.affected))
    direct_level = parse_release_level(args.labels_json)
    replacements: dict[str, str] = {}
    for module in affected:
        level = direct_level if module["path"] in direct_paths else "patch"
        current = module["version"]
        candidate = bump_version(current, level)
        if current.endswith("-SNAPSHOT") and version_key(current) >= version_key(candidate):
            continue
        replacements[module["versionAlias"]] = candidate

    changed = update_versions(Path(args.version_file), replacements)
    write_github_output({"changed": "true" if changed else "false"})
    if replacements:
        print(json.dumps(replacements, ensure_ascii=False, indent=2))


def promote_stable(args: argparse.Namespace) -> None:
    affected, _ = load_affected(Path(args.affected))
    replacements: dict[str, str] = {}
    for module in affected:
        version = module["version"]
        if not version.endswith("-SNAPSHOT"):
            continue
        replacements[module["versionAlias"]] = version.removesuffix("-SNAPSHOT")
    update_versions(Path(args.version_file), replacements)
    if replacements:
        print(json.dumps(replacements, ensure_ascii=False, indent=2))


def next_snapshots(args: argparse.Namespace) -> None:
    affected, _ = load_affected(Path(args.affected))
    replacements: dict[str, str] = {}
    for module in affected:
        version = module["version"]
        if version.endswith("-SNAPSHOT"):
            continue
        replacements[module["versionAlias"]] = bump_version(version, "patch")
    update_versions(Path(args.version_file), replacements)
    if replacements:
        print(json.dumps(replacements, ensure_ascii=False, indent=2))


def assert_versions(args: argparse.Namespace) -> None:
    affected, _ = load_affected(Path(args.affected))
    expected_snapshot = args.require == "snapshot"
    invalid = [
        f"{module['artifactId']}={module['version']}"
        for module in affected
        if module["version"].endswith("-SNAPSHOT") != expected_snapshot
    ]
    if invalid:
        raise SystemExit(f"Unexpected module versions for {args.require}: {', '.join(invalid)}")


def snapshot_notes(args: argparse.Namespace) -> None:
    existing = Path(args.existing_body).read_text() if args.existing_body and Path(args.existing_body).exists() else ""
    modules = json.loads(args.modules_json)
    assets = json.loads(args.assets_json) if args.assets_json else []
    history_lines = []
    if "## History" in existing:
        history_lines = existing.split("## History", 1)[1].strip().splitlines()
    latest_history = f"- {args.published_at} {args.commit} {args.run_url}"
    history = [latest_history, *[line for line in history_lines if line.strip() and line.strip() != latest_history]][:20]

    module_lines = [
        f"  - {module['artifactId']}: {args.group}:{module['artifactId']}:{module['version']}"
        for module in modules
    ]
    asset_lines = [
        f"  - {asset['name']}\n    sha256: {asset['sha256']}"
        for asset in assets
    ]
    body = "\n".join(
        [
            "## Latest SNAPSHOT",
            f"- Version: {args.version}",
            f"- Commit: {args.commit}",
            f"- Branch: {args.branch}",
            f"- Workflow run: {args.run_url}",
            f"- Published at: {args.published_at}",
            "- Modules:",
            *module_lines,
            "- Assets:",
            *(asset_lines or ["  - none"]),
            "",
            "## History",
            *history,
            "",
        ]
    )
    Path(args.output).write_text(body)


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
        with urllib.request.urlopen(request) as response:
            if response.status not in {204, 404}:
                raise SystemExit(f"Unexpected GitHub API delete status: {response.status}")
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return
        raise


def delete_package_version(args: argparse.Namespace) -> None:
    token = os.environ.get("GH_AUTOMATION_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        raise SystemExit("GH_AUTOMATION_TOKEN or GH_TOKEN is required")
    package = urllib.parse.quote(args.package_name, safe="")
    base_urls = [
        f"https://api.github.com/users/{args.owner}/packages/maven/{package}/versions",
        f"https://api.github.com/orgs/{args.owner}/packages/maven/{package}/versions",
    ]

    last_error: Exception | None = None
    for base_url in base_urls:
        try:
            versions = github_api_json(base_url, token)
        except urllib.error.HTTPError as exc:
            last_error = exc
            if exc.code == 404:
                continue
            raise
        for version in versions:
            if version.get("name") == args.version:
                github_api_delete(f"{base_url}/{version['id']}", token)
                print(f"Deleted package version {args.package_name}:{args.version}")
                return
        print(f"Package version not found: {args.package_name}:{args.version}")
        return
    if last_error:
        raise SystemExit(f"Package not found: {args.package_name}") from last_error
    raise SystemExit(f"Package not found: {args.package_name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    detect = subparsers.add_parser("detect-affected")
    detect.add_argument("--graph", required=True)
    detect.add_argument("--base")
    detect.add_argument("--head", required=True)
    detect.add_argument("--stable", action="store_true")
    detect.add_argument("--output")
    detect.set_defaults(func=detect_affected)

    bump = subparsers.add_parser("bump-snapshots")
    bump.add_argument("--affected", required=True)
    bump.add_argument("--labels-json", required=True)
    bump.add_argument("--version-file", default="gradle/libs.versions.toml")
    bump.set_defaults(func=bump_snapshots)

    promote = subparsers.add_parser("promote-stable")
    promote.add_argument("--affected", required=True)
    promote.add_argument("--version-file", default="gradle/libs.versions.toml")
    promote.set_defaults(func=promote_stable)

    next_snapshot = subparsers.add_parser("next-snapshots")
    next_snapshot.add_argument("--affected", required=True)
    next_snapshot.add_argument("--version-file", default="gradle/libs.versions.toml")
    next_snapshot.set_defaults(func=next_snapshots)

    assert_parser = subparsers.add_parser("assert-versions")
    assert_parser.add_argument("--affected", required=True)
    assert_parser.add_argument("--require", choices=("snapshot", "stable"), required=True)
    assert_parser.set_defaults(func=assert_versions)

    notes = subparsers.add_parser("snapshot-notes")
    notes.add_argument("--existing-body")
    notes.add_argument("--output", required=True)
    notes.add_argument("--group", required=True)
    notes.add_argument("--version", required=True)
    notes.add_argument("--commit", required=True)
    notes.add_argument("--branch", required=True)
    notes.add_argument("--run-url", required=True)
    notes.add_argument("--published-at", required=True)
    notes.add_argument("--modules-json", required=True)
    notes.add_argument("--assets-json", default="[]")
    notes.set_defaults(func=snapshot_notes)

    delete_package = subparsers.add_parser("delete-package-version")
    delete_package.add_argument("--owner", required=True)
    delete_package.add_argument("--package-name", required=True)
    delete_package.add_argument("--version", required=True)
    delete_package.set_defaults(func=delete_package_version)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        sys.stderr.write(exc.stderr)
        raise SystemExit(exc.returncode) from exc
