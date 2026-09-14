package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId

interface AdmissionSlotRepositoryPort {

    // Create
    suspend fun provision(roomId: RoomId, count: Int, participantId: ParticipantId)

    // Read

    // Update

    // Delete

}