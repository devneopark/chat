package com.devneopark.chat.restapi.shared.infrastructure

import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object SharedPostgresContainer {

    val container: PostgreSQLContainer by lazy {
        val imageName = DockerImageName.parse("postgres:16.13")
        PostgreSQLContainer(imageName)
            .withDatabaseName("chat")
            .withUsername("chat_local")
            .also { it.start() }
    }

}