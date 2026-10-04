package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 채팅방이 폐쇄되어 더 이상 참여할 수 없게 되었음을 나타내는 도메인 이벤트.
 *
 * @param roomId 폐쇄된 채팅방의 식별자.
 * @param hostParticipantId 폐쇄 시 퇴장한 호스트 참여자의 식별자.
 * @param hostUserId 폐쇄 시 퇴장한 호스트 사용자의 식별자.
 * @param closedAt 채팅방 폐쇄가 완료된 시각.
 */
data class RoomClosed(

    /**
     * 폐쇄된 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 폐쇄 시 퇴장한 호스트 참여자의 식별자.
     */
    val hostParticipantId: String,

    /**
     * 폐쇄 시 퇴장한 호스트 사용자의 식별자.
     */
    val hostUserId: String,

    /**
     * 채팅방 폐쇄가 완료된 시각.
     */
    val closedAt: Instant

)
