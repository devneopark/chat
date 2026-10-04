package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 기존 호스트가 퇴장하고 다른 참여자에게 호스트가 승계되었음을 나타내는 도메인 이벤트.
 *
 * @param roomId 호스트가 퇴장한 채팅방의 식별자.
 * @param previousHostParticipantId 퇴장한 기존 호스트 참여자의 식별자.
 * @param previousHostUserId 퇴장한 기존 호스트 사용자의 식별자.
 * @param newHostParticipantId 호스트를 승계한 참여자의 식별자.
 * @param newHostUserId 호스트를 승계한 사용자의 식별자.
 * @param transferredAt 호스트 승계가 완료된 시각.
 */
data class HostLeft(

    /**
     * 호스트가 퇴장한 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 퇴장한 기존 호스트 참여자의 식별자.
     */
    val previousHostParticipantId: String,

    /**
     * 퇴장한 기존 호스트 사용자의 식별자.
     */
    val previousHostUserId: String,

    /**
     * 호스트를 승계한 참여자의 식별자.
     */
    val newHostParticipantId: String,

    /**
     * 호스트를 승계한 사용자의 식별자.
     */
    val newHostUserId: String,

    /**
     * 호스트 승계가 완료된 시각.
     */
    val transferredAt: Instant

)
