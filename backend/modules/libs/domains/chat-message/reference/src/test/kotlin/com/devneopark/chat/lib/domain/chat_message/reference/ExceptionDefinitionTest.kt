package com.devneopark.chat.lib.domain.chat_message.reference

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
        assertEquals("1-005-001", ExceptionDefinition.CHAT_MESSAGE_ID_REQUIRED.code)
        assertEquals("Chat message ID is required.", ExceptionDefinition.CHAT_MESSAGE_ID_REQUIRED.message)

        assertEquals("1-005-002", ExceptionDefinition.MESSAGE_PAYLOAD_REQUIRED.code)
        assertEquals("Message content is required.", ExceptionDefinition.MESSAGE_PAYLOAD_REQUIRED.message)

        assertEquals("1-005-003", ExceptionDefinition.INVALID_MENTION_START_INDEX_NUMBER.code)
        assertEquals("Mention start index is invalid.", ExceptionDefinition.INVALID_MENTION_START_INDEX_NUMBER.message)

        assertEquals("1-005-004", ExceptionDefinition.INVALID_MENTION_END_INDEX_NUMBER.code)
        assertEquals("Mention end index is invalid.", ExceptionDefinition.INVALID_MENTION_END_INDEX_NUMBER.message)

        assertEquals("1-005-005", ExceptionDefinition.INVALID_MENTION_RANGE.code)
        assertEquals("Mention range is invalid.", ExceptionDefinition.INVALID_MENTION_RANGE.message)

        assertEquals("1-005-006", ExceptionDefinition.OVERLAPPING_MENTION_RANGES.code)
        assertEquals("Mention ranges must not overlap.", ExceptionDefinition.OVERLAPPING_MENTION_RANGES.message)

        assertEquals("1-005-007", ExceptionDefinition.SELF_THREAD_ROOT_NOT_ALLOWED.code)
        assertEquals(
            "Chat message cannot be its own thread root.",
            ExceptionDefinition.SELF_THREAD_ROOT_NOT_ALLOWED.message
        )

        assertEquals("1-005-008", ExceptionDefinition.SELF_REPLY_NOT_ALLOWED.code)
        assertEquals("Chat message cannot reply to itself.", ExceptionDefinition.SELF_REPLY_NOT_ALLOWED.message)
    }

}
