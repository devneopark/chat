import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

data class Dependency(
    val artifactId: String,
    val version: String,
)

data class Module(
    val path: String,
    val artifactId: String,
    val type: String,
    val baseVersion: String,
    val dependencies: List<Dependency>,
)

val repositoryRoot = Path.of("").toAbsolutePath().normalize()
val backendRoot = repositoryRoot.resolve("backend").normalize()
val outputPath = args
    .toList()
    .windowed(2)
    .firstOrNull { it[0] == "--output" }
    ?.get(1)
    ?.let(repositoryRoot::resolve)
    ?: repositoryRoot.resolve("backend/build/affected-modules.tsv")

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
    println("::error title=영향 모듈 계산 실패::${escapeCommandValue(message)}")
    appendSummary("영향 모듈 계산 실패", message)
    exitProcess(1)
}

fun notice(title: String, message: String) {
    println("::notice title=$title::${escapeCommandValue(message)}")
    appendSummary(title, message)
}

fun runGit(vararg arguments: String): Pair<Int, String> {
    val process = ProcessBuilder("git", *arguments)
        .redirectErrorStream(true)
        .start()
    val output = String(process.inputStream.readBytes(), StandardCharsets.UTF_8)
    return process.waitFor() to output
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

fun readChangedFiles(): List<String> {
    val baseSha = System.getenv("BASE_SHA").orEmpty()
    val headSha = System.getenv("HEAD_SHA").orEmpty()

    if (baseSha.isBlank() || headSha.isBlank()) {
        fail("PR 기준 커밋 정보를 찾을 수 없습니다.")
    }

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

fun moduleInfo(buildFile: Path): Module {
    val projectPath = backendRoot
        .relativize(buildFile.parent)
        .toString()
        .replace(File.separatorChar, '/')
    val segments = projectPath.split('/')

    if (segments.size < 3 || segments[0] != "modules") {
        fail("지원하지 않는 백엔드 모듈 경로입니다: backend/$projectPath")
    }

    val type = when (segments[1]) {
        "libs" -> "library"
        "services" -> "service"
        else -> fail("모듈은 modules/libs 또는 modules/services 아래에 있어야 합니다: $projectPath")
    }

    val artifactSegments = if (type == "library") {
        segments.drop(2).map { if (it == "domains") "domain" else it }
    } else {
        segments.drop(2)
    }
    val artifactId = artifactSegments.joinToString("-")
    val content = Files.readString(buildFile, StandardCharsets.UTF_8)
    val version = Regex("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"")
        .find(content)
        ?.groupValues
        ?.get(1)
        ?: fail("모듈 버전을 찾을 수 없습니다: backend/$projectPath/build.gradle.kts")
    val dependencies = Regex(
        """com\.devneopark\.chat\.backend:([A-Za-z0-9_.-]+):(\d+\.\d+\.\d+(?:-SNAPSHOT)?)"""
    )
        .findAll(content)
        .map { match ->
            Dependency(
                artifactId = match.groupValues[1],
                version = match.groupValues[2],
            )
        }
        .distinct()
        .sortedBy(Dependency::artifactId)
        .toList()

    return Module(
        path = "backend/$projectPath",
        artifactId = artifactId,
        type = type,
        baseVersion = version.removeSuffix("-SNAPSHOT"),
        dependencies = dependencies,
    )
}

val modules = Files.walk(backendRoot).use { paths ->
    paths
        .filter { path ->
            path.fileName.toString() == "build.gradle.kts" &&
                path.parent != backendRoot
        }
        .map(::moduleInfo)
        .sorted(compareBy(Module::path))
        .toList()
}

if (modules.map(Module::artifactId).distinct().size != modules.size) {
    fail("백엔드 모듈 artifactId가 중복되었습니다.")
}

val modulesByArtifact = modules.associateBy(Module::artifactId)
val changedFiles = readChangedFiles()
val commonBuildFiles = setOf(
    "backend/build.gradle.kts",
    "backend/settings.gradle.kts",
    "backend/gradle.properties",
)

val directModules = modules.filter { module ->
    changedFiles.any { path ->
        path == module.path || path.startsWith("${module.path}/")
    }
}
    .toMutableSet()

val unmatchedModuleChanges = changedFiles
    .filter { path -> path.startsWith("backend/modules/") }
    .filterNot { path ->
        modules.any { module ->
            path == module.path || path.startsWith("${module.path}/")
        }
    }

if (unmatchedModuleChanges.isNotEmpty()) {
    fail(
        "Gradle 모듈로 확인할 수 없는 백엔드 모듈 파일이 변경되었습니다: " +
            unmatchedModuleChanges.joinToString(", ")
    )
}

if (changedFiles.any { path ->
        path in commonBuildFiles || path.startsWith("backend/gradle/")
    }) {
    directModules += modules
}

val affectedModules = directModules.toMutableSet()
var changed = true
while (changed) {
    changed = false
    modules.forEach { module ->
        val dependsOnAffectedModule = module.dependencies.any { dependency ->
            modulesByArtifact[dependency.artifactId]?.let { it in affectedModules } == true
        }
        if (dependsOnAffectedModule && affectedModules.add(module)) {
            changed = true
        }
    }
}

val orderedModules = modules.filter { it in affectedModules }
val output = buildString {
    appendLine("# 모듈 경로\tartifactId\t유형\t기본 버전\t내부 의존성")
    orderedModules.forEach { module ->
        val dependencies = module.dependencies.joinToString(",") {
            val dependencyType = modulesByArtifact[it.artifactId]?.type ?: "unknown"
            "${it.artifactId}=${it.version}:$dependencyType"
        }
        appendLine(
            listOf(
                module.path,
                module.artifactId,
                module.type,
                module.baseVersion,
                dependencies,
            ).joinToString("\t")
        )
    }
}

outputPath.parent.toFile().mkdirs()
outputPath.toFile().writeText(output, StandardCharsets.UTF_8)

val outputFile = System.getenv("GITHUB_OUTPUT")
    ?.takeIf(String::isNotBlank)
if (outputFile != null) {
    val affectedProjects = orderedModules.joinToString(" ") { module ->
        val projectType = if (module.type == "library") "libs" else "services"
        ":$projectType:${module.artifactId}:build"
    }
    File(outputFile).appendText(
        "has_affected=${orderedModules.isNotEmpty()}\n" +
            "affected_modules=${orderedModules.joinToString(",") { it.artifactId }}\n" +
            "affected_projects=$affectedProjects\n",
        StandardCharsets.UTF_8,
    )
}

if (orderedModules.isEmpty()) {
    notice("영향 모듈 없음", "모듈 빌드에 영향을 주는 변경이 없습니다.")
} else {
    notice(
        "영향 모듈 계산 완료",
        "직접 변경 및 다운스트림 영향 모듈 ${orderedModules.size}개를 계산했습니다.",
    )
}
