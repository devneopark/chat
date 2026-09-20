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

}
