package com.devneopark.chat.restapi.context.room.application.port.outbound

interface PasswordHasher {

    suspend fun hash(rawPassword: String): String

    suspend fun matches(rawPassword: String, passwordHash: String): Boolean

}
