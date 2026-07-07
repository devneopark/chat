import java.util.concurrent.TimeUnit
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

data class ReleaseModule(
    val path: String,
    val projectDir: String,
    val artifactId: String,
    val versionAlias: String,
    val type: String,
    val tagNamespace: String,
)

val releaseGroup = "com.devneopark.chat.backend"
val githubPackagesUrl = "https://maven.pkg.github.com/devneopark/chat"

fun releaseModuleFor(project: Project): ReleaseModule {
    val directorySegments = project.projectDir
        .relativeTo(rootProject.projectDir)
        .invariantSeparatorsPath
        .split('/')
    require(directorySegments.size >= 3 && directorySegments[0] == "modules") {
        "Unsupported backend module directory: ${project.projectDir}"
    }

    val type = when (directorySegments[1]) {
        "libs" -> "library"
        "services" -> "service"
        else -> throw GradleException("Module must be under modules/libs or modules/services: ${project.projectDir}")
    }
    val artifactSegments = if (type == "library") {
        directorySegments.drop(2).map { segment -> if (segment == "domains") "domain" else segment }
    } else {
        directorySegments.drop(2)
    }
    val artifactId = artifactSegments.joinToString("-")
    require(artifactId.isNotBlank()) { "Cannot derive artifactId from ${project.path}" }
    require(project.name == artifactId) {
        "Gradle project name must match backend artifactId: ${project.path} != $artifactId"
    }

    return ReleaseModule(
        path = project.path,
        projectDir = project.projectDir.relativeTo(rootProject.projectDir).invariantSeparatorsPath,
        artifactId = artifactId,
        versionAlias = "backend-$artifactId",
        type = type,
        tagNamespace = if (type == "library") "backend/libs" else "backend/services",
    )
}

val releaseModules = subprojects
    .filter { project ->
        project.buildFile.isFile &&
            (project.path.startsWith(":libs:") || project.path.startsWith(":services:"))
    }
    .map(::releaseModuleFor)
    .sortedBy(ReleaseModule::path)
val releaseModulesByPath = releaseModules.associateBy(ReleaseModule::path)
val releaseModulesByArtifactId = releaseModules.associateBy(ReleaseModule::artifactId)
require(releaseModulesByArtifactId.size == releaseModules.size) {
    "Duplicate backend artifactId detected"
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val semverPattern = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")

fun versionFor(alias: String): String =
    versionCatalog.findVersion(alias)
        .orElseThrow { GradleException("Version catalog alias not found: $alias") }
        .requiredVersion

releaseModules.forEach { module ->
    val baseVersion = versionFor(module.versionAlias)
    require(semverPattern.matches(baseVersion)) {
        "Backend module version must be suffix-free SemVer: ${module.versionAlias}=$baseVersion"
    }
    if (module.type == "library") {
        val library = versionCatalog.findLibrary(module.versionAlias)
            .orElseThrow { GradleException("Version catalog library alias not found: ${module.versionAlias}") }
            .get()
        require(library.module.group == releaseGroup && library.module.name == module.artifactId) {
            "Version catalog coordinate mismatch for ${module.path}: " +
                "expected $releaseGroup:${module.artifactId}, actual ${library.module}"
        }
    }
}

val expectedVersionAliases = releaseModules.map(ReleaseModule::versionAlias).toSet()
val orphanVersionAliases = versionCatalog.versionAliases
    .filter { it.startsWith("backend-") && it !in expectedVersionAliases }
require(orphanVersionAliases.isEmpty()) {
    "Version catalog contains aliases without backend modules: ${orphanVersionAliases.sorted()}"
}

val expectedLibraryAliases = releaseModules
    .filter { it.type == "library" }
    .map(ReleaseModule::versionAlias)
    .toSet()
val orphanLibraryAliases = versionCatalog.libraryAliases
    .filter { it.startsWith("backend-") && it !in expectedLibraryAliases }
require(orphanLibraryAliases.isEmpty()) {
    "Version catalog contains library aliases without backend library modules: ${orphanLibraryAliases.sorted()}"
}

val releaseChannel = providers.gradleProperty("releaseChannel")
    .orElse("snapshot")
    .map(String::lowercase)
    .get()
require(releaseChannel in setOf("snapshot", "stable")) {
    "releaseChannel must be snapshot or stable: $releaseChannel"
}

fun channelVersion(baseVersion: String): String =
    if (releaseChannel == "snapshot") "$baseVersion-SNAPSHOT" else baseVersion

val useLocalModules = providers.gradleProperty("useLocalModules")
    .map(String::toBoolean)
    .orElse(false)
val githubPackagesUsername = providers
    .gradleProperty("githubPackagesUsername")
    .orElse(providers.environmentVariable("GH_PACKAGES_USERNAME"))
    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
val githubPackagesToken = providers
    .gradleProperty("githubPackagesToken")
    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
val releaseCommit = providers.environmentVariable("GITHUB_SHA").orElse("local")

fun String.jsonEscape(): String =
    buildString {
        this@jsonEscape.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

fun String.asJsonString(): String = "\"${jsonEscape()}\""

group = releaseGroup

allprojects {
    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri(githubPackagesUrl)
            credentials {
                username = githubPackagesUsername.orNull
                password = githubPackagesToken.orNull
            }
            content {
                includeGroup(releaseGroup)
            }
        }
    }
}

subprojects {
    val releaseModule = releaseModulesByPath[path] ?: return@subprojects

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
    }

    group = releaseGroup
    version = channelVersion(versionFor(releaseModule.versionAlias))

    extensions.configure<JavaPluginExtension>("java") {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
        resolutionStrategy.eachDependency {
            if (requested.group == releaseGroup && requested.version != null) {
                useVersion(channelVersion(requested.version!!))
                because("Backend module versions use the workflow release channel.")
            }
        }

        if (useLocalModules.get()) {
            resolutionStrategy.dependencySubstitution {
                releaseModules.forEach { candidate ->
                    substitute(module("$releaseGroup:${candidate.artifactId}"))
                        .using(project(candidate.path))
                        .because("PR validation uses backend modules from the current checkout.")
                }
            }
        }
    }

    if (releaseModule.type == "library") {
        apply {
            plugin("maven-publish")
        }

        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
        }

        tasks.named<Jar>("jar") {
            archiveBaseName.set(releaseModule.artifactId)
            archiveVersion.set(project.version.toString())
        }

        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifactId = releaseModule.artifactId
                    versionMapping {
                        usage("java-api") {
                            fromResolutionResult()
                        }
                        usage("java-runtime") {
                            fromResolutionResult()
                        }
                    }
                    pom {
                        properties.put("release.commit", releaseCommit)
                    }
                }
            }

            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri(githubPackagesUrl)
                    credentials {
                        username = githubPackagesUsername.orNull
                        password = githubPackagesToken.orNull
                    }
                }
            }
        }
    }

    if (releaseModule.type == "service") {
        apply {
            plugin("org.jetbrains.kotlin.plugin.spring")
            plugin("org.springframework.boot")
            plugin("io.spring.dependency-management")
        }

        extensions.configure<SpringBootExtension>("springBoot") {
            buildInfo()
        }

        tasks.named<Jar>("jar") {
            enabled = false
        }

        tasks.named<BootJar>("bootJar") {
            archiveFileName.set("backend-${releaseModule.artifactId}-${project.version}.jar")
        }

        dependencies {
            add("implementation", "org.springframework.boot:spring-boot-starter-webmvc")
            add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
            add("implementation", "tools.jackson.module:jackson-module-kotlin")
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        }
    }
}

tasks.register("printModuleGraph") {
    group = "release"
    description = "Prints backend release module metadata and declared dependencies as JSON."

    doLast {
        val modulesJson = releaseModules.joinToString(",\n") { releaseModule ->
            val moduleProject = project(releaseModule.path)
            val declaredDependencies = listOf("api", "implementation")
                .flatMap { configurationName ->
                    moduleProject.configurations
                        .findByName(configurationName)
                        ?.dependencies
                        ?.toList()
                        .orEmpty()
                }

            val internalDependencies = declaredDependencies
                .mapNotNull { dependency ->
                    when (dependency) {
                        is ExternalModuleDependency -> {
                            if (dependency.group == releaseGroup) {
                                val target = releaseModulesByArtifactId[dependency.name]
                                val requestedVersion = dependency.version.orEmpty()
                                val targetPath = target?.path.orEmpty()
                                val targetArtifactId = target?.artifactId ?: dependency.name
                                """
                                {
                                  "path": ${targetPath.asJsonString()},
                                  "artifactId": ${targetArtifactId.asJsonString()},
                                  "requestedVersion": ${requestedVersion.asJsonString()},
                                  "effectiveVersion": ${channelVersion(requestedVersion).asJsonString()},
                                  "registered": ${target != null}
                                }
                                """.trimIndent()
                            } else {
                                null
                            }
                        }
                        is ProjectDependency -> {
                            val target = releaseModulesByPath[dependency.path]
                            """
                            {
                              "path": ${dependency.path.asJsonString()},
                              "artifactId": ${(target?.artifactId ?: dependency.name).asJsonString()},
                              "requestedVersion": "",
                              "effectiveVersion": ${(target?.let { project(it.path).version.toString() } ?: "").asJsonString()},
                              "registered": ${target != null}
                            }
                            """.trimIndent()
                        }
                        else -> null
                    }
                }
                .distinct()
                .sorted()
                .joinToString(prefix = "[", postfix = "]")

            val externalDependencies = declaredDependencies
                .filterIsInstance<ExternalModuleDependency>()
                .filter { dependency -> dependency.group != releaseGroup }
                .map { dependency ->
                    listOf(dependency.group.orEmpty(), dependency.name, dependency.version.orEmpty())
                        .joinToString(":")
                }
                .distinct()
                .sorted()
                .joinToString(prefix = "[", postfix = "]") { it.asJsonString() }

            """
            {
              "path": ${releaseModule.path.asJsonString()},
              "projectDir": ${releaseModule.projectDir.asJsonString()},
              "artifactId": ${releaseModule.artifactId.asJsonString()},
              "versionAlias": ${releaseModule.versionAlias.asJsonString()},
              "baseVersion": ${versionFor(releaseModule.versionAlias).asJsonString()},
              "version": ${moduleProject.version.toString().asJsonString()},
              "type": ${releaseModule.type.asJsonString()},
              "tagNamespace": ${releaseModule.tagNamespace.asJsonString()},
              "dependencies": $internalDependencies,
              "externalDependencies": $externalDependencies
            }
            """.trimIndent()
        }

        val buildPlugins = linkedMapOf(
            "kotlin" to versionFor("kotlin"),
            "spring-boot" to versionFor("spring-boot"),
            "spring-dependency-management" to versionFor("spring-dependency-management"),
        ).entries.joinToString(prefix = "{", postfix = "}") { (name, version) ->
            "${name.asJsonString()}: ${version.asJsonString()}"
        }

        println(
            """
            {
              "schemaVersion": 2,
              "group": ${releaseGroup.asJsonString()},
              "githubPackagesUrl": ${githubPackagesUrl.asJsonString()},
              "releaseChannel": ${releaseChannel.asJsonString()},
              "buildPlugins": $buildPlugins,
              "modules": [
            $modulesJson
              ]
            }
            """.trimIndent(),
        )
    }
}
