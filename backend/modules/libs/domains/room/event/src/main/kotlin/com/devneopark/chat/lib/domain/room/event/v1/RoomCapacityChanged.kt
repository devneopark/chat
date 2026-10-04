package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 채팅방의 최대 참여 인원이 변경되었음을 나타내는 도메인 이벤트.
 *
 * @param roomId 정원이 변경된 채팅방의 식별자.
 * @param previousCapacity 변경 전 정원.
 * @param currentCapacity 변경 후 정원.
 * @param changedAt 정원 변경이 완료된 시각.
 */
data class RoomCapacityChanged(

    /**
     * 정원이 변경된 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 변경 전 정원.
     */
    val previousCapacity: Int,

    /**
     * 변경 후 정원.
     */
    val currentCapacity: Int,

    /**
     * 정원 변경이 완료된 시각.
     */
    val changedAt: Instant

)
