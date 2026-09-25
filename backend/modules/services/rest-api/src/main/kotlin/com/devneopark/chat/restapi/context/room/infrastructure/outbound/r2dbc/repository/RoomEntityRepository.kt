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

    @Query(
        """
        select
            id,
            title,
            password_hash
        from room
        where id = :id
            and closed_at is null
        for share
        """
    )
    suspend fun findActiveByIdForRead(id: String): RoomEntity?

    @Query(
        """
        select
            id,
            title,
            password_hash
        from room
        where id = :id
            and closed_at is null
        for update
        """
    )
    suspend fun findActiveByIdForUpdate(id: String): RoomEntity?

    @Query(
        """
        select exists(
            select 1
            from room
            where title = :title
                and id <> :roomId
                and closed_at is null
        )
        """
    )
    suspend fun existsByTitleExceptRoomId(title: String, roomId: String): Boolean

    @Modifying
    @Query(
        """
        update room
        set title = :#{#entity.title},
            password_hash = :#{#entity.passwordHash}
        where id = :#{#entity.id}
            and closed_at is null
        """
    )
    suspend fun updateRoom(entity: RoomEntity)

}
