package com.devneopark.chat.lib.domain.user.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserCredentialValidatorTest {

    @Test
    fun `given matching principal when principal is checked then true is returned`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val principal = "neo_123"

        // when
        val isValid = validator.isValidPrincipal(principal)

        // then
        assertTrue(isValid)
    }

    @Test
    fun `given non matching principal when principal is checked then false is returned`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val principal = "Neo-123"

        // when
        val isValid = validator.isValidPrincipal(principal)

        // then
        assertFalse(isValid)
    }

    @Test
    fun `given matching password when password is checked then true is returned`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val password = "Passw0rd!"

        // when
        val isValid = validator.isValidPassword(password)

        // then
        assertTrue(isValid)
    }

    @Test
    fun `given non matching password when password is checked then false is returned`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val password = "short"

        // when
        val isValid = validator.isValidPassword(password)

        // then
        assertFalse(isValid)
    }

}
