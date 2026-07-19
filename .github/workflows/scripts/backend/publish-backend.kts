import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

data class Dependency(
    val artifactId: String,
)

data class Module(
    val path: String,
    val artifactId: String,
    val type: String,
    val baseVersion: String,
    val dependencies: List<Dependency>,
)

data class CommandResult(
    val exitCode: Int,
    val output: String,
)

val channel = System.getenv("RELEASE_CHANNEL")
    ?.lowercase()
    ?.takeIf { it == "snapshot" || it == "stable" }
    ?: run {
        println("::error title=백엔드 배포 실패::배포 채널은 snapshot 또는 stable이어야 합니다.")
        exitProcess(1)
    }
val inputPath = File(
    System.getenv("AFFECTED_MODULES_FILE")
        ?: "backend/build/affected-modules.tsv"
)
val repositoryRoot = File(".").canonicalFile
val backendRoot = File(repositoryRoot, "backend")
val commitSha = System.getenv("GITHUB_SHA")
    ?.takeIf(String::isNotBlank)
    ?: runCommand(repositoryRoot, "git", "rev-parse", "HEAD").output.trim()

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
    println("::error title=백엔드 배포 실패::$message")
    appendSummary("백엔드 배포 실패", message)
    exitProcess(1)
}

fun runCommand(directory: File, vararg arguments: String): CommandResult {
    val process = ProcessBuilder(*arguments)
        .directory(directory)
        .redirectErrorStream(true)
        .start()
    val output = String(process.inputStream.readBytes(), StandardCharsets.UTF_8)
    return CommandResult(process.waitFor(), output)
}

fun requireCommand(directory: File, vararg arguments: String): String {
    val result = runCommand(directory, *arguments)
    if (result.exitCode != 0) {
        fail("명령을 실행하지 못했습니다: ${arguments.joinToString(" ")}")
    }
    return result.output
}

fun parseModules(): List<Module> {
    if (!inputPath.isFile) {
        fail("영향 모듈 계산 결과가 없습니다: ${inputPath.path}")
    }

    return inputPath.readLines(StandardCharsets.UTF_8)
        .filter { it.isNotBlank() && !it.startsWith("#") }
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
                    Dependency(dependency.substring(0, separator))
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

fun versionFor(module: Module): String =
    if (channel == "snapshot") "${module.baseVersion}-SNAPSHOT" else module.baseVersion

fun projectPathFor(module: Module): String =
    ":${if (module.type == "library") "libs" else "services"}:${module.artifactId}"

fun tagFor(module: Module): String =
    "backend/${if (module.type == "library") "libs" else "services"}/${module.artifactId}/v${versionFor(module)}"

fun orderModules(modules: List<Module>): List<Module> {
    val byArtifact = modules.associateBy(Module::artifactId)
    val remaining = modules.toMutableSet()
    val ordered = mutableListOf<Module>()

    while (remaining.isNotEmpty()) {
        val layer = remaining.filter { module ->
            module.dependencies.none { dependency ->
                byArtifact[dependency.artifactId]?.let { it in remaining } == true
            }
        }

        if (layer.isEmpty()) {
            fail("영향 모듈 의존성에 순환이 있습니다.")
        }

        ordered += layer.sortedBy(Module::path)
        remaining.removeAll(layer.toSet())
    }

    return ordered
}

fun ensureTag(module: Module) {
    val tag = tagFor(module)
    val existing = runCommand(
        repositoryRoot,
        "git",
        "ls-remote",
        "--exit-code",
        "--tags",
        "origin",
        "refs/tags/$tag",
    )

    if (channel == "stable" && existing.exitCode == 0) {
        fail("stable 태그가 이미 존재합니다: $tag")
    }

    if (channel == "snapshot") {
        requireCommand(repositoryRoot, "git", "tag", "-f", tag, commitSha)
        requireCommand(repositoryRoot, "git", "push", "--force", "origin", "refs/tags/$tag")
    } else if (existing.exitCode != 0) {
        requireCommand(repositoryRoot, "git", "tag", tag, commitSha)
        requireCommand(repositoryRoot, "git", "push", "origin", "refs/tags/$tag")
    }
}

fun publishService(module: Module) {
    val version = versionFor(module)
    val jar = File(
        module.path,
        "build/libs/backend-${module.artifactId}-$version.jar"
    )
    if (!jar.isFile) {
        fail("서비스 실행 파일을 찾을 수 없습니다: ${jar.path}")
    }

    val tag = tagFor(module)
    val title = "backend/services/${module.artifactId} $version"
    val existing = runCommand(repositoryRoot, "gh", "release", "view", tag)

    if (channel == "snapshot" && existing.exitCode == 0) {
        requireCommand(
            repositoryRoot,
            "gh",
            "release",
            "edit",
            tag,
            "--prerelease",
            "--title",
            title,
            "--notes",
            "Backend SNAPSHOT: $commitSha",
        )
        requireCommand(repositoryRoot, "gh", "release", "upload", tag, jar.path, "--clobber")
    } else {
        if (channel == "stable" && existing.exitCode == 0) {
            fail("stable Release가 이미 존재합니다: $tag")
        }
        val createArguments = mutableListOf(
            "gh", "release", "create", tag, jar.path,
            "--verify-tag", "--title", title,
            "--notes", "Backend ${channel.uppercase()}: $commitSha",
        )
        if (channel == "snapshot") createArguments += "--prerelease"
        requireCommand(repositoryRoot, *createArguments.toTypedArray())
    }
}

val modules = orderModules(parseModules())
if (modules.isEmpty()) {
    println("::notice title=백엔드 배포 건너뜀::배포할 영향 모듈이 없습니다.")
    exitProcess(0)
}

modules.forEach { module ->
    val task = if (module.type == "library") "publish" else "bootJar"
    println("::notice title=백엔드 모듈 빌드::${module.artifactId} ${versionFor(module)}")
    requireCommand(
        backendRoot,
        "sh",
        "gradlew",
        "--no-daemon",
        "-PreleaseChannel=$channel",
        "${projectPathFor(module)}:$task",
    )

    ensureTag(module)
    if (module.type == "service") publishService(module)
}

val message = "${modules.size}개 영향 모듈을 ${channel.uppercase()} 채널로 배포했습니다."
println("::notice title=백엔드 배포 완료::$message")
appendSummary("백엔드 배포 완료", message)
