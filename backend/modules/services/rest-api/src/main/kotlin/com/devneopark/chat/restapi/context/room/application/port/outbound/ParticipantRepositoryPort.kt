package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.participant.model.Participant

interface ParticipantRepositoryPort {

    // Create
    suspend fun insert(participant: Participant): Participant

    // Read

    // Update

    // Delete

}