package com.devneopark.chat.lib.domain.room.service

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomInfoValidatorTest {

    @Test
    fun `given matching title when title is validated then validation succeeds`() {
        // given
        val titleRegex = Regex("[A-Za-z0-9 ]{1,50}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = RoomInfoValidator(titleRegex, passwordRegex)
        val title = "Room 123"

        // when
        validator.validateTitle(title)
    }

    @Test
    fun `given non matching title when title is validated then domain rule violation exception is thrown`() {
        // given
        val titleRegex = Regex("[A-Za-z0-9 ]{1,50}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = RoomInfoValidator(titleRegex, passwordRegex)
        val title = "Room_123"
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            validator.validateTitle(title)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given matching password when password is validated then validation succeeds`() {
        // given
        val titleRegex = Regex("[A-Za-z0-9 ]{1,50}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = RoomInfoValidator(titleRegex, passwordRegex)
        val password = "Passw0rd!"

        // when
        validator.validatePassword(password)
    }

    @Test
    fun `given non matching password when password is validated then domain rule violation exception is thrown`() {
        // given
        val titleRegex = Regex("[A-Za-z0-9 ]{1,50}")
        val passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
        val validator = RoomInfoValidator(titleRegex, passwordRegex)
        val password = "short"
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            validator.validatePassword(password)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
