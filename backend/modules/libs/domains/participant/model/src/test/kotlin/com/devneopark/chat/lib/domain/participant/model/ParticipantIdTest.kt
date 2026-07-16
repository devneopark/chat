package com.devneopark.chat.lib.domain.participant.model

import com.devneopark.chat.lib.domain.participant.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParticipantIdTest {

    @Test
    fun `given value when participant id is created then value is preserved`() {
        // given
        val value = "participant-1"

        // when
        val id = Participant.Id(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given value when participant id is created from factory then value is preserved`() {
        // given
        val value = "participant-1"

        // when
        val id: ParticipantId = Participant.Id.from(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given blank value when participant id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.PARTICIPANT_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Participant.Id(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
