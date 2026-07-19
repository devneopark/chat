package com.devneopark.chat.lib.domain.user.service

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserProfileValidatorTest {

    @Test
    fun `given matching display name when display name is validated then validation succeeds`() {
        // given
        val displayNameRegex = Regex("[A-Za-z0-9]{2,20}")
        val validator = UserProfileValidator(displayNameRegex)
        val displayName = "Neo123"

        // when
        validator.validateDisplayName(displayName)
    }

    @Test
    fun `given non matching display name when display name is validated then domain rule violation exception is thrown`() {
        // given
        val displayNameRegex = Regex("[A-Za-z0-9]{2,20}")
        val validator = UserProfileValidator(displayNameRegex)
        val displayName = "Neo Park"
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            validator.validateDisplayName(displayName)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
