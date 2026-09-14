package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.room.model.Room

interface RoomRepositoryPort {

    // Create
    suspend fun insert(room: Room): Room

    // Read
    suspend fun existsByTitle(title: String): Boolean

    // Update

    // Delete

}