package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class ParticipantEntityTest {

    @Test
    fun `Participant를 ParticipantEntity로 변환한다`() {
        // given
        val joinedAt = Instant.parse("2026-09-15T00:00:00Z")
        val participant = Participant(
            Participant.Id("participant-001"),
            Room.Id("room-001"),
            User.Id("user-001"),
            ParticipantRole.HOST,
            joinedAt
        )

        // when
        val entity = ParticipantEntity.from(participant)

        // then
        assertEquals("participant-001", entity.id)
        assertEquals("room-001", entity.roomId)
        assertEquals("user-001", entity.userId)
        assertEquals("HOST", entity.role)
        assertEquals(joinedAt.toJavaInstant(), entity.joinedAt)
    }

    @Test
    fun `ParticipantEntity를 Participant로 변환한다`() {
        // given
        val entity = ParticipantEntity().apply {
            id = "participant-001"
            roomId = "room-001"
            userId = "user-001"
            role = "HOST"
            joinedAt = java.time.Instant.parse("2026-09-15T00:00:00Z")
        }

        // when
        val participant = entity.toDomain()

        // then
        assertEquals("participant-001", participant.id.value)
        assertEquals("room-001", participant.roomId.value)
        assertEquals("user-001", participant.userId.value)
        assertEquals(ParticipantRole.HOST, participant.role)
        assertEquals(entity.joinedAt, participant.joinedAt.toJavaInstant())
    }

}
