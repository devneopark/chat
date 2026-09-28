package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.crypto

import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordVerifier
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/** BCrypt로 저장된 사용자 비밀번호를 검증하는 IAM 인프라 어댑터다. */
@Component
class BCryptUserPasswordVerifier(

    private val passwordEncoder: PasswordEncoder

) : PasswordVerifier {

    override suspend fun matches(rawPassword: String, hash: String): Boolean {
        return passwordEncoder.matches(rawPassword, hash)
    }

}
