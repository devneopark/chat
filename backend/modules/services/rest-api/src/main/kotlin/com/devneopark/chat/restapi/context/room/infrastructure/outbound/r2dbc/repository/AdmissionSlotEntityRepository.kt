package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model.AdmissionSlotEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdmissionSlotEntityRepository : CoroutineCrudRepository<AdmissionSlotEntity, String> {

    @Query(
        """
        select count(*)
        from admission_slot
        where room_id = :roomId
        """
    )
    suspend fun countByRoomId(roomId: String): Long

    @Query(
        """
        select coalesce(max(slot_number), 0)
        from admission_slot
        where room_id = :roomId
        """
    )
    suspend fun findMaxSlotNumberByRoomId(roomId: String): Int

    @Query(
        """
        select
            room_id,
            slot_number,
            occupant_participant_id
        from admission_slot
        where room_id = :roomId
            and occupant_participant_id is null
        order by slot_number desc
        limit :limit
        for update skip locked
        """
    )
    suspend fun findEmptyByRoomIdForUpdateSkipLocked(
        roomId: String,
        limit: Int
    ): List<AdmissionSlotEntity>

    @Query(
        """
        select
            room_id,
            slot_number,
            occupant_participant_id
        from admission_slot
        where room_id = :roomId
            and occupant_participant_id is null
        order by slot_number asc
        limit 1
        for update skip locked
        """
    )
    suspend fun findFirstEmptyByRoomIdForUpdateSkipLocked(roomId: String): AdmissionSlotEntity?

    @Modifying
    @Query(
        """
        update admission_slot
        set occupant_participant_id = :participantId
        where room_id = :roomId
            and slot_number = :slotNumber
            and occupant_participant_id is null
        """
    )
    suspend fun assignParticipant(
        roomId: String,
        slotNumber: Int,
        participantId: String
    )

    @Modifying
    @Query(
        """
        update admission_slot
        set occupant_participant_id = null
        where occupant_participant_id = :participantId
        """
    )
    suspend fun releaseParticipant(participantId: String)

}
