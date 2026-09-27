package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model.ParticipantEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ParticipantEntityRepository : CoroutineCrudRepository<ParticipantEntity, String> {

    @Modifying
    @Query(
        """
        insert into participant(
            id,
            room_id,
            user_id,
            participant_role,
            joined_at
        ) values (
            :#{#entity.id},
            :#{#entity.roomId},
            :#{#entity.userId},
            :#{#entity.role},
            :#{#entity.joinedAt}
        )
        """
    )
    suspend fun insert(entity: ParticipantEntity)

    @Query(
        """
        select
            id,
            room_id,
            user_id,
            participant_role,
            joined_at
        from participant
        where room_id = :roomId
            and user_id = :userId
            and participant_role = 'HOST'
            and exited_at is null
        for share
        """
    )
    suspend fun findActiveHostForRead(roomId: String, userId: String): ParticipantEntity?

    @Query(
        """
        select
            id,
            room_id,
            user_id,
            participant_role,
            joined_at
        from participant
        where room_id = :roomId
            and user_id = :userId
            and exited_at is null
        for share
        """
    )
    suspend fun findActiveByRoomIdAndUserIdForRead(
        roomId: String,
        userId: String
    ): ParticipantEntity?

    @Query(
        """
        select
            id,
            room_id,
            user_id,
            participant_role,
            joined_at
        from participant
        where room_id = :roomId
            and user_id in (:requesterUserId, :targetUserId)
            and exited_at is null
        order by user_id
        for update
        """
    )
    suspend fun findActiveByRoomIdAndUserIdsForUpdate(
        roomId: String,
        requesterUserId: String,
        targetUserId: String
    ): List<ParticipantEntity>

    @Modifying
    @Query(
        """
        update participant
        set participant_role = :role
        where id = :id
            and exited_at is null
        """
    )
    suspend fun updateRole(id: String, role: String)

}
