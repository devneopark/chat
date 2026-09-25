package com.devneopark.chat.restapi.context.room.application.port.outbound

import com.devneopark.chat.lib.domain.admission_slot.reference.AdmissionSlotId
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId

interface AdmissionSlotRepositoryPort {

    // Create
    suspend fun provision(roomId: RoomId, count: Int, participantId: ParticipantId)

    suspend fun provisionAdditionalSlots(
        roomId: RoomId,
        currentMaxSlotNumber: Int,
        count: Int
    )

    // Read
    suspend fun countByRoomId(roomId: RoomId): Int

    suspend fun findMaxSlotNumberByRoomId(roomId: RoomId): Int

    suspend fun findEmptyByRoomIdForUpdateSkipLocked(
        roomId: RoomId,
        limit: Int
    ): List<AdmissionSlotId>

    // Update

    // Delete
    /**
     * 슬롯 식별자로 슬롯을 일괄 삭제한다.
     *
     * 정원 규모가 커지면 배열 크기와 트랜잭션 시간을 고려해 청크 처리 방식을 검토한다.
     */
    suspend fun deleteAllByIds(slots: List<AdmissionSlotId>)

}
