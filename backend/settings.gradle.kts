rootProject.name = "chat"

fun artifactIdFor(moduleDirectory: File): Pair<String, String> {
    val segments = moduleDirectory
        .relativeTo(rootDir)
        .invariantSeparatorsPath
        .split('/')
    require(segments.size >= 3 && segments[0] == "modules") {
        "Unsupported backend module directory: $moduleDirectory"
    }
    val type = when (segments[1]) {
        "libs" -> "libs"
        "services" -> "services"
        else -> error("Module must be under modules/libs or modules/services: $moduleDirectory")
    }
    val artifactSegments = if (type == "libs") {
        segments.drop(2).map { segment -> if (segment == "domains") "domain" else segment }
    } else {
        segments.drop(2)
    }
    return type to artifactSegments.joinToString("-")
}

val moduleDirectories = listOf(
    rootDir.resolve("modules/libs"),
    rootDir.resolve("modules/services"),
)
    .filter(File::isDirectory)
    .flatMap { moduleRoot ->
        moduleRoot
            .walkTopDown()
            .filter { directory ->
                directory.isDirectory && directory.resolve("build.gradle.kts").isFile
            }
            .toList()
    }
    .distinct()
    .sortedBy { it.relativeTo(rootDir).invariantSeparatorsPath }

val registeredProjectPaths = mutableSetOf<String>()
moduleDirectories.forEach { moduleDirectory ->
    val (type, artifactId) = artifactIdFor(moduleDirectory)
    val projectPath = ":$type:$artifactId"
    require(registeredProjectPaths.add(projectPath)) {
        "Duplicate backend artifactId: $artifactId"
    }
    include(projectPath)
    project(projectPath).projectDir = moduleDirectory
}

if (findProject(":libs") != null) {
    project(":libs").projectDir = rootDir.resolve("modules/libs")
}
if (findProject(":services") != null) {
    project(":services").projectDir = rootDir.resolve("modules/services")
}
