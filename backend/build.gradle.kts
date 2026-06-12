plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.devneopark"

allprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

subprojects {
    if (path.startsWith(":modules:services:")) {
        apply {
            plugin("org.jetbrains.kotlin.plugin.spring")
            plugin("org.springframework.boot")
            plugin("io.spring.dependency-management")
        }

        tasks.named("jar") {
            enabled = false
        }

        dependencies {
            implementation("org.springframework.boot:spring-boot-starter-webmvc")
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            implementation("tools.jackson.module:jackson-module-kotlin")
            testImplementation("org.springframework.boot:spring-boot-starter-test")
        }
    }
}
