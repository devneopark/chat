package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.time.Instant

interface ParticipantRepositoryPort {

    // Create
    suspend fun insert(participant: Participant): Participant

    // Read
    suspend fun existsActiveHostForRead(roomId: RoomId, userId: UserId): Boolean

    suspend fun findActiveByRoomIdAndUserIdForRead(roomId: RoomId, userId: UserId): Participant?

    suspend fun findActiveByRoomIdAndUserIdForUpdate(roomId: RoomId, userId: UserId): Participant?

    suspend fun findOldestActiveGuestForUpdate(roomId: RoomId): Participant?

    suspend fun findActiveByRoomIdAndUserIdsForUpdate(
        roomId: RoomId,
        requesterUserId: UserId,
        targetUserId: UserId
    ): List<Participant>

    // Update
    suspend fun updateRole(participantId: ParticipantId, role: ParticipantRole)

    suspend fun markExited(participantId: ParticipantId, exitedAt: Instant)

    // Delete

}
