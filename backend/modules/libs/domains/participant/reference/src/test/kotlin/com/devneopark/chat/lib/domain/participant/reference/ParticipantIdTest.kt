package com.devneopark.chat.lib.domain.participant.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantIdTest {

    @Test
    fun `given participant id implementation when values are read then original values are exposed`() {
        // given
        val roomId = "room-1"
        val userId = "user-1"
        val participantId: ParticipantId = TestParticipantId(roomId, userId)

        // when
        val actualRoomId = participantId.roomId
        val actualUserId = participantId.userId

        // then
        assertEquals(roomId, actualRoomId)
        assertEquals(userId, actualUserId)
    }

    private data class TestParticipantId(
        override val roomId: String,
        override val userId: String
    ) : ParticipantId

}
