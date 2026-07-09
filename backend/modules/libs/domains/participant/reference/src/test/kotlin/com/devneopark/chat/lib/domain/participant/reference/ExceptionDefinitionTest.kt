package com.devneopark.chat.lib.domain.participant.reference

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
        assertEquals("1-004-001", ExceptionDefinition.ROOM_ID_REQUIRED.code)
        assertEquals("Room ID is required.", ExceptionDefinition.ROOM_ID_REQUIRED.message)

        assertEquals("1-004-002", ExceptionDefinition.USER_ID_REQUIRED.code)
        assertEquals("User ID is required.", ExceptionDefinition.USER_ID_REQUIRED.message)
    }

}
