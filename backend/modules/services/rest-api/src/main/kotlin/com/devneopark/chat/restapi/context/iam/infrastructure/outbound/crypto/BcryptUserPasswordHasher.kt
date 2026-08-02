package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.crypto

import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptUserPasswordHasher(

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

) : PasswordHasher {

    override suspend fun hash(rawPassword: String): String {
        val hash = passwordEncoder.encode(rawPassword)
        return hash ?: "null"
    }

    override suspend fun matches(rawPassword: String, hash: String): Boolean {
        return passwordEncoder.matches(rawPassword, hash)
    }

}