package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model.ParticipantEntity
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository.ParticipantEntityRepository
import org.springframework.stereotype.Repository

@Repository
class ParticipantRepositoryAdapter(

    private val participantEntityRepository: ParticipantEntityRepository

) : ParticipantRepositoryPort {

    override suspend fun insert(participant: Participant): Participant {
        participantEntityRepository.insert(ParticipantEntity.from(participant))
        return participant
    }

    override suspend fun existsActiveHostForRead(roomId: RoomId, userId: UserId): Boolean {
        val activeHost = participantEntityRepository.findActiveHostForRead(roomId.value, userId.value)
        return activeHost != null
    }

    override suspend fun findActiveByRoomIdAndUserIdForRead(roomId: RoomId, userId: UserId): Participant? {
        return participantEntityRepository
            .findActiveByRoomIdAndUserIdForRead(roomId.value, userId.value)
            ?.toDomain()
    }

    override suspend fun findActiveByRoomIdAndUserIdsForUpdate(
        roomId: RoomId,
        requesterUserId: UserId,
        targetUserId: UserId
    ): List<Participant> {
        return participantEntityRepository
            .findActiveByRoomIdAndUserIdsForUpdate(
                roomId.value,
                requesterUserId.value,
                targetUserId.value
            )
            .map(ParticipantEntity::toDomain)
    }

    override suspend fun updateRole(participantId: ParticipantId, role: ParticipantRole) {
        participantEntityRepository.updateRole(participantId.value, role.name)
    }

}
