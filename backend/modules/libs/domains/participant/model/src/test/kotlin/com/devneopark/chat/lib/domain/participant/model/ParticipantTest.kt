package com.devneopark.chat.lib.domain.participant.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Instant

class ParticipantTest {

    @Test
    fun `given id role and joined at when participant is created then values are preserved`() {
        // given
        val id = Participant.Id("room-1", "user-1")
        val role = ParticipantRole.GUEST
        val joinedAt = Instant.parse("2026-07-10T00:00:00Z")

        // when
        val participant = Participant(id, role, joinedAt)

        // then
        assertSame(id, participant.id)
        assertEquals(role, participant.role)
        assertEquals(joinedAt, participant.joinedAt)
    }

    @Test
    fun `given guest participant when participant is promoted to host then role is host`() {
        // given
        val participant = Participant(Participant.Id("room-1", "user-1"), ParticipantRole.GUEST)

        // when
        participant.promoteToHost()

        // then
        assertEquals(ParticipantRole.HOST, participant.role)
    }

    @Test
    fun `given host participant when participant is demoted to guest then role is guest`() {
        // given
        val participant = Participant(Participant.Id("room-1", "user-1"), ParticipantRole.HOST)

        // when
        participant.demoteToGuest()

        // then
        assertEquals(ParticipantRole.GUEST, participant.role)
    }

}
