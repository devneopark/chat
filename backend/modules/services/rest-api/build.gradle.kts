version = "0.0.1"

dependencies {
    // project libraries
    implementation("com.devneopark.chat.backend:shared-kernel:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat.backend:shared-applications-exception:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat.backend:shared-applications-identifier:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat.backend:shared-domain-exception:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat.backend:domain-user-reference:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat.backend:domain-user-model:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat.backend:domain-user-service:0.0.1-SNAPSHOT")

    // spring
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.2")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")

    // kotlin
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.testcontainers:testcontainers-r2dbc")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // external libraries
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
}
