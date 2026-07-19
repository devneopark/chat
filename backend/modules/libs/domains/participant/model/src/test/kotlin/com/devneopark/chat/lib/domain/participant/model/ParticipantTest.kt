package com.devneopark.chat.lib.domain.participant.model

import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Instant

class ParticipantTest {

    @Test
    fun `given id room user role and joined at when participant is created then values are preserved`() {
        // given
        val id = Participant.Id("participant-1")
        val roomId = TestRoomId("room-1")
        val userId = TestUserId("user-1")
        val role = ParticipantRole.GUEST
        val joinedAt = Instant.parse("2026-07-10T00:00:00Z")

        // when
        val participant = Participant(id, roomId, userId, role, joinedAt)

        // then
        assertSame(id, participant.id)
        assertSame(roomId, participant.roomId)
        assertSame(userId, participant.userId)
        assertEquals(role, participant.role)
        assertEquals(joinedAt, participant.joinedAt)
    }

    @Test
    fun `given guest participant when participant is promoted to host then role is host`() {
        // given
        val participant = createParticipant(ParticipantRole.GUEST)

        // when
        participant.promoteToHost()

        // then
        assertEquals(ParticipantRole.HOST, participant.role)
    }

    @Test
    fun `given host participant when participant is demoted to guest then role is guest`() {
        // given
        val participant = createParticipant(ParticipantRole.HOST)

        // when
        participant.demoteToGuest()

        // then
        assertEquals(ParticipantRole.GUEST, participant.role)
    }

    private fun createParticipant(role: ParticipantRole): Participant {
        return Participant(
            Participant.Id("participant-1"),
            TestRoomId("room-1"),
            TestUserId("user-1"),
            role
        )
    }

    private data class TestRoomId(override val value: String) : RoomId

    private data class TestUserId(override val value: String) : UserId

}
