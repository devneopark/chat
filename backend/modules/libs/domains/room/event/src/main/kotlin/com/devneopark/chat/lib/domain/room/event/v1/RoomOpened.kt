package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 채팅방이 생성되어 참여할 수 있는 상태가 되었음을 나타내는 도메인 이벤트.
 *
 * @param roomId 생성된 채팅방의 식별자.
 * @param hostParticipantId 초기 호스트 참여자의 식별자.
 * @param hostUserId 초기 호스트 사용자의 식별자.
 * @param openedAt 채팅방 생성이 완료된 시각.
 */
data class RoomOpened(

    /**
     * 생성된 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 초기 호스트 참여자의 식별자.
     */
    val hostParticipantId: String,

    /**
     * 초기 호스트 사용자의 식별자.
     */
    val hostUserId: String,

    /**
     * 채팅방 생성이 완료된 시각.
     */
    val openedAt: Instant

)
