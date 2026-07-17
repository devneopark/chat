package com.devneopark.chat.lib.domain.chat_message.model

import com.devneopark.chat.lib.domain.chat_message.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MentionTest {

    @Test
    fun `given mention values when mention is created then values are preserved`() {
        // given
        val participantId = TestParticipantId("participant-1")
        val startInclusive = 1
        val endExclusive = 4

        // when
        val mention = Mention(participantId, startInclusive, endExclusive)

        // then
        assertSame(participantId, mention.participantId)
        assertEquals(startInclusive, mention.startInclusive)
        assertEquals(endExclusive, mention.endExclusive)
    }

    @Test
    fun `given negative start index when mention is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_START_INDEX_NUMBER

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Mention(TestParticipantId("participant-1"), -1, 1)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given negative end index when mention is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_END_INDEX_NUMBER

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Mention(TestParticipantId("participant-1"), 0, -1)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given empty range when mention is created then domain rule violation exception is thrown`() {
        assertInvalidRange(1, 1)
    }

    @Test
    fun `given reversed range when mention is created then domain rule violation exception is thrown`() {
        assertInvalidRange(2, 1)
    }

    private fun assertInvalidRange(startInclusive: Int, endExclusive: Int) {
        // given
        val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_RANGE

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Mention(
                TestParticipantId("participant-1"),
                startInclusive,
                endExclusive
            )
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    private data class TestParticipantId(
        override val value: String
    ) : ParticipantId

}
