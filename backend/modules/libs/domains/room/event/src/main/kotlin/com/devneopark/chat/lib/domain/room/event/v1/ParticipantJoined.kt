package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 사용자가 채팅방에 입장했음을 나타내는 도메인 이벤트.
 *
 * @param roomId 입장한 채팅방의 식별자.
 * @param participantId 입장으로 생성된 참여자의 식별자.
 * @param userId 입장한 사용자의 식별자.
 * @param role 입장 시 부여된 참여자의 역할.
 * @param joinedAt 입장이 완료된 시각.
 */
data class ParticipantJoined(

    /**
     * 입장한 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 입장으로 생성된 참여자의 식별자.
     */
    val participantId: String,

    /**
     * 입장한 사용자의 식별자.
     */
    val userId: String,

    /**
     * 입장 시 부여된 참여자의 역할.
     */
    val role: String,

    /**
     * 입장이 완료된 시각.
     */
    val joinedAt: Instant

)
