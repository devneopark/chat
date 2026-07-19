from __future__ import annotations

import unittest
from unittest.mock import patch

import module_release


def dependency(path: str, artifact_id: str) -> module_release.Dependency:
    return module_release.Dependency(
        path=path,
        artifact_id=artifact_id,
        requested_version="0.1.0",
        effective_version="0.1.0-SNAPSHOT",
        registered=True,
    )


def backend_module(
    path: str,
    artifact_id: str,
    *dependencies: module_release.Dependency,
    base_version: str = "0.1.0",
) -> module_release.Module:
    project_dir = path.removeprefix(":").replace(":", "/")
    return module_release.Module(
        path=path,
        project_dir=project_dir,
        artifact_id=artifact_id,
        base_version=base_version,
        version=f"{base_version}-SNAPSHOT",
        type="library",
        tag_namespace="backend/libs",
        dependencies=tuple(dependencies),
        external_dependencies=(),
    )


def graph(*modules: module_release.Module, plugins: tuple[tuple[str, str], ...] = ()) -> module_release.Graph:
    return module_release.Graph(
        group="com.devneopark.chat.backend",
        build_plugins=plugins,
        modules=tuple(modules),
    )


class ReleasePlanTest(unittest.TestCase):
    @patch("module_release.latest_stable_tag", return_value=None)
    def test_changed_upstream_propagates_and_builds_dependency_layers(self, _: object) -> None:
        kernel = backend_module(":modules:libs:shared:kernel", "shared-kernel")
        exception = backend_module(
            ":modules:libs:shared:domains:exception",
            "shared-domain-exception",
            dependency(kernel.path, kernel.artifact_id),
        )
        model = backend_module(
            ":modules:libs:domains:room:model",
            "domain-room-model",
            dependency(exception.path, exception.artifact_id),
        )
        service = backend_module(
            ":modules:libs:domains:room:service",
            "domain-room-service",
            dependency(model.path, model.artifact_id),
        )
        current = graph(kernel, exception, model, service)

        plan = module_release.build_plan(
            current,
            current,
            ["modules/libs/shared/kernel/src/main/kotlin/Kernel.kt"],
        )

        self.assertEqual(
            [[module["artifactId"] for module in layer] for layer in plan["layers"]],
            [
                ["shared-kernel"],
                ["shared-domain-exception"],
                ["domain-room-model"],
                ["domain-room-service"],
            ],
        )

    @patch("module_release.latest_stable_tag", return_value=None)
    def test_common_plugin_change_affects_every_module(self, _: object) -> None:
        first = backend_module(":modules:libs:first", "first")
        second = backend_module(":modules:libs:second", "second")
        base = graph(first, second, plugins=(("kotlin", "2.3.20"),))
        head = graph(first, second, plugins=(("kotlin", "2.3.21"),))

        plan = module_release.build_plan(base, head, ["gradle/libs.versions.toml"])

        self.assertEqual(
            [module["artifactId"] for module in plan["affected"]],
            ["first", "second"],
        )

    @patch("module_release.latest_stable_tag", return_value=None)
    def test_deleted_module_is_reported_without_republishing_consumers(self, _: object) -> None:
        removed = backend_module(":modules:libs:removed", "removed")
        survivor = backend_module(":modules:libs:survivor", "survivor")

        plan = module_release.build_plan(
            graph(removed, survivor),
            graph(survivor),
            ["modules/libs/removed/build.gradle.kts"],
        )

        self.assertEqual([module["artifactId"] for module in plan["deleted"]], ["removed"])
        self.assertEqual(plan["affected"], [])

    @patch("module_release.latest_stable_tag", return_value=None)
    def test_project_path_migration_is_not_a_module_deletion(self, _: object) -> None:
        base = backend_module(":modules:libs:shared:kernel", "shared-kernel")
        head = backend_module(":libs:shared-kernel", "shared-kernel")

        plan = module_release.build_plan(graph(base), graph(head), [])

        self.assertEqual(plan["deleted"], [])
        self.assertEqual([module["artifactId"] for module in plan["affected"]], ["shared-kernel"])

    @patch(
        "module_release.latest_stable_tag",
        return_value=("0.1.0", "backend/libs/shared-kernel/v0.1.0"),
    )
    @patch("module_release.github_annotation")
    def test_declared_module_version_must_be_greater_than_stable(
        self,
        _: object,
        __: object,
    ) -> None:
        kernel = backend_module(":modules:libs:shared:kernel", "shared-kernel")

        with self.assertRaisesRegex(SystemExit, "must be greater"):
            module_release.build_plan(
                graph(kernel),
                graph(kernel),
                ["modules/libs/shared/kernel/src/main/kotlin/Kernel.kt"],
            )

    @patch(
        "module_release.latest_stable_tag",
        return_value=("0.1.0", "backend/libs/shared-kernel/v0.1.0"),
    )
    @patch("module_release.git_output", return_value="release-sha")
    def test_same_stable_tag_at_head_is_resumable(self, _: object, __: object) -> None:
        kernel = backend_module(":libs:shared-kernel", "shared-kernel")

        plan = module_release.build_plan(
            graph(kernel),
            graph(kernel),
            ["libs/shared-kernel/src/main/kotlin/Kernel.kt"],
            resumable_sha="release-sha",
        )

        self.assertEqual([module["artifactId"] for module in plan["affected"]], ["shared-kernel"])

    @patch("module_release.latest_stable_tag", return_value=None)
    def test_dependency_cycle_fails_with_module_paths(self, _: object) -> None:
        first_path = ":libs:first"
        second_path = ":libs:second"
        first = backend_module(first_path, "first", dependency(second_path, "second"))
        second = backend_module(second_path, "second", dependency(first_path, "first"))

        with self.assertRaisesRegex(SystemExit, "Dependency cycle detected"):
            module_release.build_plan(
                graph(first, second),
                graph(first, second),
                ["libs/first/src/main/kotlin/First.kt"],
            )

    @patch("module_release.latest_stable_tag", return_value=None)
    @patch("module_release.github_annotation")
    def test_pr_single_module_policy_rejects_multiple_content_modules(
        self,
        _: object,
        __: object,
    ) -> None:
        first = backend_module(":libs:first", "first")
        second = backend_module(":libs:second", "second")

        with self.assertRaisesRegex(SystemExit, "only one backend module"):
            module_release.build_plan(
                graph(first, second),
                graph(first, second),
                [
                    "libs/first/src/main/kotlin/First.kt",
                    "libs/second/src/test/kotlin/SecondTest.kt",
                ],
                enforce_single_pr_module=True,
            )

    @patch("module_release.github_annotation")
    def test_pr_scope_rejects_common_and_module_files(
        self,
        _: object,
    ) -> None:
        first = backend_module(":libs:first", "first")

        with self.assertRaisesRegex(SystemExit, "Common files must be changed"):
            module_release.build_plan(
                graph(first),
                graph(first),
                [
                    "libs/first/src/main/kotlin/First.kt",
                    "build.gradle.kts",
                ],
                enforce_pr_scope=True,
            )

    @patch("module_release.latest_stable_tag", return_value=None)
    def test_pr_scope_allows_common_files_only(self, _: object) -> None:
        first = backend_module(":libs:first", "first")
        second = backend_module(":libs:second", "second")

        plan = module_release.build_plan(
            graph(first, second),
            graph(first, second),
            ["build.gradle.kts", "gradle/libs.versions.toml"],
            enforce_pr_scope=True,
        )

        self.assertEqual(
            [module["artifactId"] for module in plan["affected"]],
            ["first", "second"],
        )

    @patch("module_release.latest_stable_tag", return_value=None)
    def test_pr_single_module_policy_allows_other_module_build_gradle_changes(self, _: object) -> None:
        first = backend_module(":libs:first", "first")
        second = backend_module(":libs:second", "second")

        plan = module_release.build_plan(
            graph(first, second),
            graph(first, second),
            [
                "libs/first/src/main/kotlin/First.kt",
                "libs/second/build.gradle.kts",
            ],
            enforce_single_pr_module=True,
        )

        self.assertEqual(
            [module["artifactId"] for module in plan["affected"]],
            ["first", "second"],
        )

    @patch("module_release.latest_stable_tag", return_value=None)
    @patch("module_release.github_annotation")
    def test_pr_single_module_policy_counts_module_version_changes(
        self,
        _: object,
        __: object,
    ) -> None:
        first = backend_module(":libs:first", "first", base_version="0.1.0")
        first_bumped = backend_module(":libs:first", "first", base_version="0.1.1")
        second = backend_module(":libs:second", "second", base_version="0.1.0")
        second_bumped = backend_module(":libs:second", "second", base_version="0.1.1")

        with self.assertRaisesRegex(SystemExit, "module-version-changed"):
            module_release.build_plan(
                graph(first, second),
                graph(first_bumped, second_bumped),
                [
                    "libs/first/build.gradle.kts",
                    "libs/second/build.gradle.kts",
                ],
                enforce_single_pr_module=True,
            )

    @patch("module_release.latest_stable_tag", return_value=None)
    @patch("module_release.github_annotation")
    def test_pr_single_module_policy_rejects_multiple_added_modules(
        self,
        _: object,
        __: object,
    ) -> None:
        first = backend_module(":libs:first", "first")
        second = backend_module(":libs:second", "second")

        with self.assertRaisesRegex(SystemExit, "module-added"):
            module_release.build_plan(
                graph(),
                graph(first, second),
                [
                    "libs/first/build.gradle.kts",
                    "libs/second/build.gradle.kts",
                ],
                enforce_single_pr_module=True,
            )


if __name__ == "__main__":
    unittest.main()
