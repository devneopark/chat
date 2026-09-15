package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model.RoomEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface RoomEntityRepository : CoroutineCrudRepository<RoomEntity, String> {

    @Modifying
    @Query(
        """
        insert into room(
            id,
            title,
            password_hash
        ) values (
            :#{#entity.id},
            :#{#entity.title},
            :#{#entity.passwordHash}
        )
        """
    )
    suspend fun insert(entity: RoomEntity)

    @Query(
        """
        select exists(
            select 1
            from room
            where title = :title
              and closed_at is null
        )
        """
    )
    suspend fun existsByTitle(title: String): Boolean

}
