package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.kernel.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileTest {

    @Test
    fun `given display name when profile is created then display name is preserved`() {
        // given
        val displayName = "Neo"

        // when
        val profile = Profile(displayName)

        // then
        assertEquals(displayName, profile.displayName)
    }

    @Test
    fun `given max length display name when profile is created then display name is preserved`() {
        // given
        val displayName = "a".repeat(20)

        // when
        val profile = Profile(displayName)

        // then
        assertEquals(displayName, profile.displayName)
    }

    @Test
    fun `given blank display name when profile is created then domain rule violation exception is thrown`() {
        // given
        val displayName = " "
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Profile(displayName)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given too long display name when profile is created then domain rule violation exception is thrown`() {
        // given
        val displayName = "a".repeat(21)
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Profile(displayName)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given new display name when display name is changed then display name is updated`() {
        // given
        val profile = Profile("Neo")
        val newDisplayName = "Park"

        // when
        profile.changeDisplayName(newDisplayName)

        // then
        assertEquals(newDisplayName, profile.displayName)
    }

    @Test
    fun `given blank display name when display name is changed then exception is thrown and original display name is retained`() {
        // given
        val originalDisplayName = "Neo"
        val profile = Profile(originalDisplayName)
        val newDisplayName = " "
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            profile.changeDisplayName(newDisplayName)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalDisplayName, profile.displayName)
    }

    @Test
    fun `given too long display name when display name is changed then exception is thrown and original display name is retained`() {
        // given
        val originalDisplayName = "Neo"
        val profile = Profile(originalDisplayName)
        val newDisplayName = "a".repeat(21)
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            profile.changeDisplayName(newDisplayName)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalDisplayName, profile.displayName)
    }

}
