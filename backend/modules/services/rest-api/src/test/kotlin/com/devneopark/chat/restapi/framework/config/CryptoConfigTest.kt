package com.devneopark.chat.restapi.framework.config

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CryptoConfigTest {

    @Test
    fun `BCrypt 기반 PasswordEncoder를 생성한다`() {
        // given
        val rawPassword = "raw-password"

        // when
        val passwordEncoder = CryptoConfig().passwordEncoder()
        val encodedPassword = passwordEncoder.encode(rawPassword)

        // then
        assertIs<BCryptPasswordEncoder>(passwordEncoder)
        assertNotEquals(rawPassword, encodedPassword)
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword))
    }

}
