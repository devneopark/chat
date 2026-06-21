import java.util.concurrent.TimeUnit
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
    val artifactId: String,
    val versionAlias: String,
    val type: String,
)

val releaseGroup = "com.devneopark.chat"
val githubPackagesUrl = "https://maven.pkg.github.com/devneopark/chat"
val releaseModules = listOf(
    ReleaseModule(":modules:libs:domains:message:event", "domain-message-event", "domain-message-event", "library"),
    ReleaseModule(":modules:libs:domains:message:model", "domain-message-model", "domain-message-model", "library"),
    ReleaseModule(":modules:libs:domains:message:reference", "domain-message-reference", "domain-message-reference", "library"),
    ReleaseModule(":modules:libs:domains:room:event", "domain-room-event", "domain-room-event", "library"),
    ReleaseModule(":modules:libs:domains:room:model", "domain-room-model", "domain-room-model", "library"),
    ReleaseModule(":modules:libs:domains:room:reference", "domain-room-reference", "domain-room-reference", "library"),
    ReleaseModule(":modules:libs:domains:user:event", "domain-user-event", "domain-user-event", "library"),
    ReleaseModule(":modules:libs:domains:user:model", "domain-user-model", "domain-user-model", "library"),
    ReleaseModule(":modules:libs:domains:user:reference", "domain-user-reference", "domain-user-reference", "library"),
    ReleaseModule(":modules:libs:domains:user:service", "domain-user-service", "domain-user-service", "library"),
    ReleaseModule(":modules:libs:shared:kernel", "shared-kernel", "shared-kernel", "library"),
    ReleaseModule(":modules:services:messaging-gateway", "messaging-gateway", "messaging-gateway", "service"),
    ReleaseModule(":modules:services:rest-api", "rest-api", "rest-api", "service"),
)
val releaseModulesByPath = releaseModules.associateBy { it.path }
val releaseModulesByArtifactId = releaseModules.associateBy { it.artifactId }
val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val useLocalModules = providers.gradleProperty("useLocalModules").map(String::toBoolean).orElse(false)
val githubPackagesUsername = providers
    .gradleProperty("githubPackagesUsername")
    .orElse(providers.environmentVariable("GH_PACKAGES_USERNAME"))
    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
val githubPackagesToken = providers
    .gradleProperty("githubPackagesToken")
    .orElse(providers.environmentVariable("GH_AUTOMATION_TOKEN"))
    .orElse(providers.environmentVariable("GITHUB_TOKEN"))

fun versionFor(alias: String): String =
    versionCatalog.findVersion(alias)
        .orElseThrow { GradleException("Version catalog alias not found: $alias") }
        .requiredVersion

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
    val releaseModule = releaseModulesByPath[path]
    if (releaseModule == null) {
        return@subprojects
    }

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
    }

    group = releaseGroup
    version = versionFor(releaseModule.versionAlias)

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

        if (useLocalModules.get()) {
            resolutionStrategy.dependencySubstitution {
                releaseModules.forEach { releaseModule ->
                    substitute(module("$releaseGroup:${releaseModule.artifactId}"))
                        .using(project(releaseModule.path))
                        .because("PR CI validates internal module changes from the current checkout.")
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
            archiveFileName.set("${releaseModule.artifactId}-${project.version}.jar")
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
    description = "Prints release module metadata and internal dependencies as JSON."

    doLast {
        val modulesJson = releaseModules.joinToString(",\n") { releaseModule ->
            val moduleProject = project(releaseModule.path)
            val projectDir = moduleProject.projectDir
                .relativeTo(rootProject.projectDir)
                .path
                .replace(File.separatorChar, '/')
            val internalDependencies = moduleProject.configurations
                .findByName("implementation")
                ?.dependencies
                ?.mapNotNull { dependency ->
                    when (dependency) {
                        is ExternalModuleDependency -> {
                            if (dependency.group == releaseGroup) {
                                releaseModulesByArtifactId[dependency.name]?.path
                            } else {
                                null
                            }
                        }
                        is ProjectDependency -> dependency.path
                        else -> null
                    }
                }
                ?.distinct()
                ?.sorted()
                .orEmpty()
            val dependencyObjects = internalDependencies.joinToString(prefix = "[", postfix = "]") { dependencyPath ->
                val dependencyModule = releaseModulesByPath.getValue(dependencyPath)
                """
                {"path":"${dependencyModule.path.jsonEscape()}","artifactId":"${dependencyModule.artifactId.jsonEscape()}"}
                """.trimIndent()
            }

            """
            {
              "path": "${releaseModule.path.jsonEscape()}",
              "projectDir": "${projectDir.jsonEscape()}",
              "artifactId": "${releaseModule.artifactId.jsonEscape()}",
              "versionAlias": "${releaseModule.versionAlias.jsonEscape()}",
              "version": "${moduleProject.version.toString().jsonEscape()}",
              "type": "${releaseModule.type.jsonEscape()}",
              "dependencies": $dependencyObjects
            }
            """.trimIndent()
        }

        println(
            """
            {
              "group": "${releaseGroup.jsonEscape()}",
              "githubPackagesUrl": "${githubPackagesUrl.jsonEscape()}",
              "modules": [
            $modulesJson
              ]
            }
            """.trimIndent()
        )
    }
}
