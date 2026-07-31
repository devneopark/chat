package com.devneopark.chat.restapi.context.iam.application.port.outbound

interface PasswordHasher {

    suspend fun hash(rawPassword: String): String

    suspend fun matches(rawPassword: String, hash: String): Boolean

}