package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model.RoomEntity
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository.RoomEntityRepository
import org.springframework.stereotype.Repository

@Repository
class RoomRepositoryAdapter(

    private val roomEntityRepository: RoomEntityRepository

) : RoomRepositoryPort {

    override suspend fun insert(room: Room): Room {
        roomEntityRepository.insert(RoomEntity.from(room))
        return room
    }

    override suspend fun existsByTitle(title: String): Boolean {
        return roomEntityRepository.existsByTitle(title)
    }

}
