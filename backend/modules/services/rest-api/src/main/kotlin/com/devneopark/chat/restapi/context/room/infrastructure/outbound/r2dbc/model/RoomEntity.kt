package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.room.model.Room
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("room")
class RoomEntity {

    @Id
    lateinit var id: String

    lateinit var title: String

    var passwordHash: String? = null

    companion object {

        fun from(room: Room): RoomEntity {
            return RoomEntity().apply {
                id = room.id.value
                title = room.title
                passwordHash = room.passwordHash
            }
        }

    }

    fun toDomain(): Room {
        return Room(
            Room.Id.from(id),
            title,
            passwordHash
        )
    }

}
