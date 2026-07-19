import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.system.exitProcess

val repositoryRoot = Path.of("").toAbsolutePath().normalize()
val backendRoot = repositoryRoot.resolve("backend").normalize()
val backendModulesRoot = backendRoot.resolve("modules").normalize()

fun escapeCommandValue(value: String): String =
    value
        .replace("%", "%25")
        .replace("\r", "%0D")
        .replace("\n", "%0A")

fun appendSummary(title: String, body: String) {
    val summaryFile = System.getenv("GITHUB_STEP_SUMMARY")
        ?.takeIf(String::isNotBlank)

    if (summaryFile != null) {
        File(summaryFile).appendText(
            "## $title\n\n$body\n\n",
            StandardCharsets.UTF_8,
        )
    }
}

fun fail(message: String): Nothing {
    println(
        "::error title=${escapeCommandValue("PR 변경 범위 검증 실패")}" +
            "::${escapeCommandValue(message)}"
    )
    appendSummary("PR 변경 범위 검증 실패", message)
    exitProcess(1)
}

fun notice(title: String, message: String) {
    println(
        "::notice title=${escapeCommandValue(title)}" +
            "::${escapeCommandValue(message)}"
    )
    appendSummary(title, message)
}

fun runGit(vararg arguments: String): Pair<Int, String> {
    val process = ProcessBuilder("git", *arguments)
        .redirectErrorStream(true)
        .start()
    val output = String(process.inputStream.readBytes(), StandardCharsets.UTF_8)
    return process.waitFor() to output
}

fun verifyCommit(sha: String, label: String) {
    val (exitCode, _) = runGit("cat-file", "-e", "$sha^{commit}")
    if (exitCode != 0) {
        fail("$label 커밋을 찾을 수 없습니다: $sha")
    }
}

fun normalizeRepositoryPath(path: String): String {
    val absolutePath = try {
        repositoryRoot.resolve(path).normalize()
    } catch (_: RuntimeException) {
        fail("유효하지 않은 변경 파일 경로입니다: $path")
    }

    if (!absolutePath.startsWith(repositoryRoot)) {
        fail("저장소 외부를 가리키는 변경 파일 경로입니다: $path")
    }

    return repositoryRoot
        .relativize(absolutePath)
        .toString()
        .replace(File.separatorChar, '/')
}

fun readChangedFiles(baseSha: String, headSha: String): List<String> {
    val (exitCode, output) = runGit(
        "diff",
        "--name-only",
        "--find-renames",
        "-z",
        "$baseSha...$headSha",
    )

    if (exitCode != 0) {
        fail("변경 파일 목록을 확인하지 못했습니다. 종료 코드: $exitCode")
    }

    return output
        .split('\u0000')
        .filter(String::isNotBlank)
        .map(::normalizeRepositoryPath)
}

fun isBackendRootFile(path: String): Boolean {
    val allowedRootFiles = setOf(
        "backend/build.gradle.kts",
        "backend/settings.gradle.kts",
        "backend/gradle.properties",
        "backend/gradlew",
        "backend/gradlew.bat",
        "backend/README.md",
    )

    return path in allowedRootFiles || path.startsWith("backend/gradle/")
}

fun isRepositoryRootFile(path: String): Boolean =
    path in setOf(
        ".gitattributes",
        ".gitignore",
        "README.md",
    )

fun isCommonFile(path: String): Boolean =
    isRepositoryRootFile(path) ||
        isBackendRootFile(path) ||
        path.startsWith(".github/workflows/")

fun modulePathFor(branch: String): String {
    val moduleRelativePath = branch.removePrefix("develop/backend/")

    if (!moduleRelativePath.startsWith("libs/") &&
        !moduleRelativePath.startsWith("services/")) {
        fail(
            "백엔드 모듈 브랜치는 develop/backend/libs/** 또는 " +
                "develop/backend/services/** 형식이어야 합니다: $branch"
        )
    }

    val moduleAbsolutePath = try {
        backendModulesRoot.resolve(moduleRelativePath).normalize()
    } catch (_: RuntimeException) {
        fail("브랜치명에서 유효한 모듈 경로를 만들 수 없습니다.")
    }

    if (!moduleAbsolutePath.startsWith(backendModulesRoot)) {
        fail("브랜치명이 백엔드 프로젝트 경로 밖을 가리킵니다.")
    }

    val modulePath = repositoryRoot
        .relativize(moduleAbsolutePath)
        .toString()
        .replace(File.separatorChar, '/')

    if (moduleRelativePath.isBlank() || !File("$modulePath/build.gradle.kts").isFile) {
        fail("브랜치명에 대응하는 Gradle 모듈이 없습니다: $modulePath")
    }

    return modulePath
}

val baseBranch = System.getenv("GITHUB_BASE_REF").orEmpty()
val headBranch = System.getenv("GITHUB_HEAD_REF").orEmpty()
val baseSha = System.getenv("BASE_SHA").orEmpty()
val headSha = System.getenv("HEAD_SHA").orEmpty()

if (baseSha.isBlank() || headSha.isBlank()) {
    fail("PR 기준 커밋 정보를 찾을 수 없습니다.")
}

verifyCommit(baseSha, "기준")
verifyCommit(headSha, "변경")

val changedFiles = readChangedFiles(baseSha, headSha)

val moduleOwnerBranch = when {
    baseBranch.startsWith("develop/backend/") &&
        headBranch.startsWith("develop/backend/") -> {
        if (baseBranch != headBranch) {
            fail(
                "서로 다른 백엔드 통합 브랜치 간 PR은 허용하지 않습니다: " +
                    "$headBranch → $baseBranch"
            )
        }
        baseBranch
    }

    baseBranch.startsWith("develop/backend/") -> baseBranch
    baseBranch == "main" && headBranch.startsWith("develop/backend/") -> headBranch
    else -> null
}

if (baseBranch != "main" && !baseBranch.startsWith("develop/backend/")) {
    fail("지원하지 않는 PR 대상 브랜치입니다: $baseBranch")
}

if (moduleOwnerBranch != null) {
    val modulePath = modulePathFor(moduleOwnerBranch)
    val invalidFiles = changedFiles.filterNot { path ->
        path == modulePath || path.startsWith("$modulePath/")
    }

    if (invalidFiles.isNotEmpty()) {
        fail(
            buildString {
                appendLine("담당 모듈 외부의 파일이 변경되었습니다.")
                invalidFiles.forEach { appendLine("- $it") }
                appendLine()
                appendLine("담당 브랜치: $moduleOwnerBranch")
                appendLine("담당 모듈: $modulePath")
                appendLine("프로젝트 루트 및 백엔드 루트 변경은 main 대상 PR로 제출해야 합니다.")
            }
        )
    }

    notice(
        "PR 변경 범위 검증 통과",
        "담당 모듈 변경 ${changedFiles.size}개가 허용 범위에 포함됩니다."
    )
} else {
    val invalidFiles = changedFiles.filterNot(::isCommonFile)

    if (invalidFiles.isNotEmpty()) {
        fail(
            buildString {
                appendLine("main 대상 공통 변경 PR에 모듈 파일이 포함되었습니다.")
                invalidFiles.forEach { appendLine("- $it") }
                appendLine()
                appendLine(
                    "모듈 변경은 해당 develop/backend/libs/** 또는 " +
                        "develop/backend/services/** 브랜치를 거쳐야 합니다."
                )
            }
        )
    }

    notice(
        "공통 파일 PR 변경 범위 검증 통과",
        "프로젝트 루트, 백엔드 루트 또는 워크플로 공통 파일만 변경되었습니다."
    )
}
