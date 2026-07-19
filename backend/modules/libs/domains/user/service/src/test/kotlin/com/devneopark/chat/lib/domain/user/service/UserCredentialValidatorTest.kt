package com.devneopark.chat.lib.domain.user.service

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserCredentialValidatorTest {

    @Test
    fun `given matching principal when principal is validated then validation succeeds`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val principal = "neo_123"

        // when
        validator.validatePrincipal(principal)
    }

    @Test
    fun `given non matching principal when principal is validated then domain rule violation exception is thrown`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val principal = "Neo-123"
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PRINCIPAL

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            validator.validatePrincipal(principal)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given matching password when password is validated then validation succeeds`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val password = "Passw0rd!"

        // when
        validator.validatePassword(password)
    }

    @Test
    fun `given non matching password when password is validated then domain rule violation exception is thrown`() {
        // given
        val principalRegex = Regex("[a-z][a-z0-9_]{2,15}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = UserCredentialValidator(principalRegex, passwordRegex)
        val password = "short"
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            validator.validatePassword(password)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
