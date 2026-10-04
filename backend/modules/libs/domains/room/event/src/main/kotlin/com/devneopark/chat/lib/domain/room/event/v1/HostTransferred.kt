package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 채팅방의 호스트가 다른 사용자로 변경되었음을 나타내는 도메인 이벤트.
 *
 * @param roomId 호스트가 변경된 채팅방의 식별자.
 * @param previousHostUserId 변경 전 호스트 사용자의 식별자.
 * @param newHostUserId 변경 후 호스트 사용자의 식별자.
 * @param transferredAt 호스트 변경이 완료된 시각.
 */
data class HostTransferred(

    /**
     * 호스트가 변경된 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 변경 전 호스트 사용자의 식별자.
     */
    val previousHostUserId: String,

    /**
     * 변경 후 호스트 사용자의 식별자.
     */
    val newHostUserId: String,

    /**
     * 호스트 변경이 완료된 시각.
     */
    val transferredAt: Instant

)
