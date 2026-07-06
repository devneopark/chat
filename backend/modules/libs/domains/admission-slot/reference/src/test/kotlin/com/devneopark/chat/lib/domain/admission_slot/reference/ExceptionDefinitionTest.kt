package com.devneopark.chat.lib.domain.admission_slot.reference

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
        assertEquals("1-003-001", ExceptionDefinition.ROOM_ID_REQUIRED.code)
        assertEquals("Room ID is required.", ExceptionDefinition.ROOM_ID_REQUIRED.message)

        assertEquals("1-003-002", ExceptionDefinition.INVALID_SLOT_NUMBER.code)
        assertEquals("Admission slot number is invalid.", ExceptionDefinition.INVALID_SLOT_NUMBER.message)

        assertEquals("1-003-003", ExceptionDefinition.ALREADY_OCCUPIED.code)
        assertEquals("Admission slot is already occupied.", ExceptionDefinition.ALREADY_OCCUPIED.message)

        assertEquals("1-003-004", ExceptionDefinition.OCCUPANT_MISMATCH.code)
        assertEquals("Admission slot occupant does not match.", ExceptionDefinition.OCCUPANT_MISMATCH.message)
    }

}
