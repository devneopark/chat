package com.devneopark.chat.restapi.context.room.infrastructure.outbound.crypto

import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptRoomPasswordHasher(

    private val passwordEncoder: PasswordEncoder

) : PasswordHasher {

    override suspend fun hash(rawPassword: String): String {
        val hash = passwordEncoder.encode(rawPassword)
        return hash ?: "null"
    }

}