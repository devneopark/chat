package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.kernel.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CredentialTest {

    @Test
    fun `given principal and password hash when credential is created then values are preserved`() {
        // given
        val principal = "user@example.com"
        val passwordHash = "hashed-password"

        // when
        val credential = Credential(principal, passwordHash)

        // then
        assertEquals(principal, credential.principal)
        assertEquals(passwordHash, credential.passwordHash)
    }

    @Test
    fun `given max length principal when credential is created then principal is preserved`() {
        // given
        val principal = "a".repeat(36)
        val passwordHash = "hashed-password"

        // when
        val credential = Credential(principal, passwordHash)

        // then
        assertEquals(principal, credential.principal)
    }

    @Test
    fun `given blank principal when credential is created then domain rule violation exception is thrown`() {
        // given
        val principal = " "
        val passwordHash = "hashed-password"
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PRINCIPAL

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Credential(principal, passwordHash)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given too long principal when credential is created then domain rule violation exception is thrown`() {
        // given
        val principal = "a".repeat(37)
        val passwordHash = "hashed-password"
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PRINCIPAL

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Credential(principal, passwordHash)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given blank password hash when credential is created then domain rule violation exception is thrown`() {
        // given
        val principal = "user@example.com"
        val passwordHash = " "
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Credential(principal, passwordHash)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given new password hash when password hash is changed then password hash is updated`() {
        // given
        val credential = Credential("user@example.com", "old-hashed-password")
        val newPasswordHash = "new-hashed-password"

        // when
        credential.changePasswordHash(newPasswordHash)

        // then
        assertEquals(newPasswordHash, credential.passwordHash)
    }

    @Test
    fun `given blank password hash when password hash is changed then exception is thrown and original password hash is retained`() {
        // given
        val originalPasswordHash = "old-hashed-password"
        val credential = Credential("user@example.com", originalPasswordHash)
        val newPasswordHash = " "
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            credential.changePasswordHash(newPasswordHash)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalPasswordHash, credential.passwordHash)
    }

}
