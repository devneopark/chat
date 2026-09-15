package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.participant.model.Participant
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

}
