import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

data class Module(
    val path: String,
    val artifactId: String,
    val type: String,
    val baseVersion: String,
    val dependencies: List<Dependency>,
)

data class Dependency(
    val artifactId: String,
    val version: String,
    val type: String,
)

data class CommandResult(
    val exitCode: Int,
    val output: String,
)

val inputPath = args
    .toList()
    .windowed(2)
    .firstOrNull { it[0] == "--input" }
    ?.get(1)
    ?.let(::File)
    ?: File("backend/build/affected-modules.tsv")

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
    println("::error title=stable 버전 검증 실패::${escapeCommandValue(message)}")
    appendSummary("stable 버전 검증 실패", message)
    exitProcess(1)
}

fun notice(title: String, message: String) {
    println("::notice title=$title::${escapeCommandValue(message)}")
    appendSummary(title, message)
}

fun runCommand(vararg arguments: String): CommandResult {
    val process = ProcessBuilder(*arguments)
        .redirectErrorStream(true)
        .start()
    val output = String(process.inputStream.readBytes(), StandardCharsets.UTF_8)
    return CommandResult(process.waitFor(), output)
}

fun parseModules(): List<Module> {
    if (!inputPath.isFile) {
        fail("영향 모듈 계산 결과 파일이 없습니다: ${inputPath.path}")
    }

    return inputPath.readLines(StandardCharsets.UTF_8)
        .dropWhile { it.startsWith("#") || it.isBlank() }
        .map { line ->
            val columns = line.split('\t')
            if (columns.size != 5) {
                fail("영향 모듈 계산 결과 형식이 올바르지 않습니다: $line")
            }

            val dependencies = columns[4]
                .takeIf(String::isNotBlank)
                ?.split(',')
                ?.map { dependency ->
                    val separator = dependency.indexOf('=')
                    if (separator <= 0) {
                        fail("내부 의존성 형식이 올바르지 않습니다: $dependency")
                    }
                    val artifactId = dependency.substring(0, separator)
                    val versionAndType = dependency.substring(separator + 1)
                    val typeSeparator = versionAndType.lastIndexOf(':')
                    if (typeSeparator <= 0) {
                        fail("내부 의존성 유형이 없습니다: $dependency")
                    }
                    Dependency(
                        artifactId = artifactId,
                        version = versionAndType.substring(0, typeSeparator),
                        type = versionAndType.substring(typeSeparator + 1),
                    )
                }
                ?: emptyList()

            Module(
                path = columns[0],
                artifactId = columns[1],
                type = columns[2],
                baseVersion = columns[3],
                dependencies = dependencies,
            )
        }
}

fun encodePathSegment(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

fun queryGitHub(endpoint: String, jq: String): List<String> {
    val result = runCommand(
        "gh",
        "api",
        "--paginate",
        endpoint,
        "--jq",
        jq,
    )

    if (result.exitCode != 0) {
        if (result.output.contains("HTTP 404")) return emptyList()
        fail("GitHub 배포 상태를 조회하지 못했습니다.")
    }

    return result.output
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
}

val repositoryOwner = System.getenv("GITHUB_REPOSITORY_OWNER").orEmpty()
val repository = System.getenv("GITHUB_REPOSITORY").orEmpty()

if (repositoryOwner.isBlank() || repository.isBlank()) {
    fail("GitHub 저장소 정보를 찾을 수 없습니다.")
}

val stableCache = mutableMapOf<String, Boolean>()

fun hasStableLibraryVersion(artifactId: String, version: String): Boolean {
    val packageNames = listOf(
        "com.devneopark.chat.backend:$artifactId",
        "com.devneopark.chat.backend.$artifactId",
    )

    return packageNames.any { packageName ->
        val cacheKey = "package:$packageName:$version"
        stableCache.getOrPut(cacheKey) {
            val encodedPackage = encodePathSegment(packageName)
            listOf("users", "orgs").any { ownerType ->
                val endpoint =
                    "$ownerType/$repositoryOwner/packages/maven/$encodedPackage/versions?per_page=100"
                queryGitHub(endpoint, ".[] | .name")
                    .any { it == version }
            }
        }
    }
}

fun hasStableServiceVersion(artifactId: String, version: String): Boolean {
    val cacheKey = "release:$artifactId:$version"
    return stableCache.getOrPut(cacheKey) {
        val expectedTag = "backend/services/$artifactId/v$version"
        queryGitHub(
            "repos/$repository/releases?per_page=100",
            ".[] | select(.draft == false and .prerelease == false) | .tag_name",
        ).any { it == expectedTag }
    }
}

fun hasStableVersion(module: Module, version: String): Boolean =
    when (module.type) {
        "library" -> hasStableLibraryVersion(module.artifactId, version)
        "service" -> hasStableServiceVersion(module.artifactId, version)
        else -> fail("지원하지 않는 모듈 유형입니다: ${module.type}")
    }

val modules = parseModules()
val collisions = mutableListOf<String>()
val snapshotDependencies = mutableListOf<String>()

modules.forEach { module ->
    if (hasStableVersion(module, module.baseVersion)) {
        collisions +=
            "${module.artifactId}: ${module.baseVersion} stable 버전이 이미 배포되어 있습니다. " +
                "${module.path}/build.gradle.kts의 버전을 올려야 합니다."
    }

    module.dependencies
        .filter { dependency -> dependency.version.endsWith("-SNAPSHOT") }
        .forEach { dependency ->
            if (dependency.type != "unknown" &&
                dependency.version.endsWith("-SNAPSHOT") &&
                hasStableVersion(
                    Module(
                        path = "",
                        artifactId = dependency.artifactId,
                        type = dependency.type,
                        baseVersion = dependency.version.removeSuffix("-SNAPSHOT"),
                        dependencies = emptyList(),
                    ),
                    dependency.version.removeSuffix("-SNAPSHOT"),
                )
            ) {
                snapshotDependencies +=
                    "${module.artifactId}가 ${dependency.artifactId}:${dependency.version} 를 사용하고 있습니다. " +
                        "stable 버전 ${dependency.version.removeSuffix("-SNAPSHOT")}으로 교체해야 합니다."
            }
        }
}

if (collisions.isNotEmpty() || snapshotDependencies.isNotEmpty()) {
    fail(
        buildString {
            collisions.forEach { appendLine("- $it") }
            snapshotDependencies.forEach { appendLine("- $it") }
        }
    )
}

notice(
    "stable 버전 검증 통과",
    "영향 모듈 ${modules.size}개의 버전과 내부 의존성 버전을 확인했습니다."
)
