package com.devneopark.chat.lib.domain.room.reference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionDefinitionTest {

    @Test
    fun `given exception definitions when inspected then code and message are not blank`() {
        // given
        val definitions = ExceptionDefinition.entries

        // when
        val blankDefinitions = definitions.filter { definition ->
            definition.code.isBlank() || definition.message.isBlank()
        }

        // then
        assertTrue(blankDefinitions.isEmpty())
    }

    @Test
    fun `given exception definitions when codes are collected then codes are unique`() {
        // given
        val definitions = ExceptionDefinition.entries

        // when
        val codes = definitions.map { it.code }
        val uniqueCodes = codes.distinct()

        // then
        assertEquals(codes.size, uniqueCodes.size)
    }

    @Test
    fun `given exception definitions when contract is checked then code and message match expected values`() {
        // then
        assertEquals("1-002-001", ExceptionDefinition.ROOM_ID_REQUIRED.code)
        assertEquals("Room ID is required.", ExceptionDefinition.ROOM_ID_REQUIRED.message)

        assertEquals("1-002-002", ExceptionDefinition.PARTICIPANT_USER_ID_REQUIRED.code)
        assertEquals("Participant user ID is required.", ExceptionDefinition.PARTICIPANT_USER_ID_REQUIRED.message)

        assertEquals("1-002-003", ExceptionDefinition.ROOM_PASSWORD_REQUIRED.code)
        assertEquals("Room password is required.", ExceptionDefinition.ROOM_PASSWORD_REQUIRED.message)

        assertEquals("1-002-004", ExceptionDefinition.INVALID_ROOM_TITLE.code)
        assertEquals("Room title is invalid.", ExceptionDefinition.INVALID_ROOM_TITLE.message)

        assertEquals("1-002-005", ExceptionDefinition.INVALID_ROOM_PASSWORD.code)
        assertEquals("Room password is invalid.", ExceptionDefinition.INVALID_ROOM_PASSWORD.message)

        assertEquals("1-002-006", ExceptionDefinition.INVALID_ROOM_CAPACITY.code)
        assertEquals("Room capacity is invalid.", ExceptionDefinition.INVALID_ROOM_CAPACITY.message)

        assertEquals("1-002-007", ExceptionDefinition.ROOM_HOST_PERMISSION_REQUIRED.code)
        assertEquals("Room host permission is required.", ExceptionDefinition.ROOM_HOST_PERMISSION_REQUIRED.message)

        assertEquals("1-002-008", ExceptionDefinition.ALREADY_JOINED_ROOM.code)
        assertEquals("Participant has already joined the room.", ExceptionDefinition.ALREADY_JOINED_ROOM.message)

        assertEquals("1-002-009", ExceptionDefinition.ROOM_CAPACITY_EXCEEDED.code)
        assertEquals("Room capacity has been exceeded.", ExceptionDefinition.ROOM_CAPACITY_EXCEEDED.message)

        assertEquals("1-002-010", ExceptionDefinition.ROOM_CAPACITY_BELOW_PARTICIPANT_COUNT.code)
        assertEquals(
            "Room capacity cannot be less than participant count.",
            ExceptionDefinition.ROOM_CAPACITY_BELOW_PARTICIPANT_COUNT.message
        )

        assertEquals("1-002-011", ExceptionDefinition.INCORRECT_ROOM_PASSWORD.code)
        assertEquals("Room password is incorrect.", ExceptionDefinition.INCORRECT_ROOM_PASSWORD.message)

        assertEquals("1-002-012", ExceptionDefinition.ROOM_NOT_FOUND.code)
        assertEquals("Room was not found.", ExceptionDefinition.ROOM_NOT_FOUND.message)

        assertEquals("1-002-013", ExceptionDefinition.PARTICIPANT_NOT_FOUND.code)
        assertEquals("Room participant was not found.", ExceptionDefinition.PARTICIPANT_NOT_FOUND.message)
    }

}
