package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.domain.user.reference.UserId

interface ParticipantRepositoryPort {

    // Create
    suspend fun insert(participant: Participant): Participant

    // Read
    suspend fun existsActiveHostForRead(roomId: RoomId, userId: UserId): Boolean

    // Update

    // Delete

}
