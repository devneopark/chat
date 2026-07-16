package com.devneopark.chat.lib.domain.participant.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantIdTest {

    @Test
    fun `given participant id implementation when value is read then original value is exposed`() {
        // given
        val value = "participant-1"
        val participantId: ParticipantId = TestParticipantId(value)

        // when
        val actualValue = participantId.value

        // then
        assertEquals(value, actualValue)
    }

    private data class TestParticipantId(
        override val value: String
    ) : ParticipantId

}
