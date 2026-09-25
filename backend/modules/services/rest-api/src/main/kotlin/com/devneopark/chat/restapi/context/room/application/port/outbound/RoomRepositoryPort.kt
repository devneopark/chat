package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.room.reference.RoomId

interface RoomRepositoryPort {

    // Create
    suspend fun insert(room: Room): Room

    // Read
    suspend fun existsByTitle(title: String): Boolean

    suspend fun findActiveByIdForRead(id: RoomId): Room?

    suspend fun findActiveByIdForUpdate(id: RoomId): Room?

    suspend fun existsByTitleExceptRoomId(title: String, roomId: RoomId): Boolean

    // Update
    suspend fun update(room: Room): Room

    // Delete

}
