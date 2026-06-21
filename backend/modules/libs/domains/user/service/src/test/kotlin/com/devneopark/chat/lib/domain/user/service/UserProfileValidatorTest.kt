package com.devneopark.chat.lib.domain.user.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserProfileValidatorTest {

    @Test
    fun `given matching display name when display name is checked then true is returned`() {
        // given
        val displayNameRegex = Regex("[A-Za-z0-9]{2,20}")
        val validator = UserProfileValidator(displayNameRegex)
        val displayName = "Neo123"

        // when
        val isValid = validator.isValidDisplayName(displayName)

        // then
        assertTrue(isValid)
    }

    @Test
    fun `given non matching display name when display name is checked then false is returned`() {
        // given
        val displayNameRegex = Regex("[A-Za-z0-9]{2,20}")
        val validator = UserProfileValidator(displayNameRegex)
        val displayName = "Neo Park"

        // when
        val isValid = validator.isValidDisplayName(displayName)

        // then
        assertFalse(isValid)
    }

}
