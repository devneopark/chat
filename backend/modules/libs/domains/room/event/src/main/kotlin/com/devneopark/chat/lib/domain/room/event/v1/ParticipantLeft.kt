package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 사용자가 채팅방에서 퇴장했음을 나타내는 도메인 이벤트.
 *
 * @param roomId 퇴장한 채팅방의 식별자.
 * @param participantId 퇴장한 참여자의 식별자.
 * @param userId 퇴장한 사용자의 식별자.
 * @param roleBeforeLeave 퇴장 직전에 참여자가 가지고 있던 역할.
 * @param leftAt 퇴장이 완료된 시각.
 */
data class ParticipantLeft(

    /**
     * 퇴장한 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 퇴장한 참여자의 식별자.
     */
    val participantId: String,

    /**
     * 퇴장한 사용자의 식별자.
     */
    val userId: String,

    /**
     * 퇴장 직전에 참여자가 가지고 있던 역할.
     */
    val roleBeforeLeave: String,

    /**
     * 퇴장이 완료된 시각.
     */
    val leftAt: Instant

)
