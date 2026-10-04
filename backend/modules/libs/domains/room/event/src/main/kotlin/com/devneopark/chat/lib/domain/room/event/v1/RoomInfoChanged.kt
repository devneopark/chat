package com.devneopark.chat.lib.domain.room.event.v1

import java.time.Instant

/**
 * 채팅방의 변경 가능한 정보가 변경되었음을 나타내는 도메인 이벤트.
 *
 * @param roomId 정보가 변경된 채팅방의 식별자.
 * @param title 변경 후 채팅방의 제목.
 * @param passwordStateTransition 비밀번호 존재 상태의 변경 유형.
 * @param changedAt 정보 변경이 완료된 시각.
 */
data class RoomInfoChanged(

    /**
     * 정보가 변경된 채팅방의 식별자.
     */
    val roomId: String,

    /**
     * 변경 후 채팅방의 제목.
     */
    val title: String,

    /**
     * 비밀번호 존재 상태의 변경 유형.
     */
    val passwordStateTransition: PasswordStateTransition,

    /**
     * 정보 변경이 완료된 시각.
     */
    val changedAt: Instant

) {

    /**
     * 채팅방 비밀번호 존재 상태의 변경 유형.
     */
    enum class PasswordStateTransition {

        /**
         * 비밀번호가 없는 상태에서 있는 상태로 변경됨.
         */
        ADDED,

        /**
         * 비밀번호가 있는 상태에서 없는 상태로 변경됨.
         */
        REMOVED,

        /**
         * 비밀번호 존재 상태가 변경되지 않음.
         */
        NONE

    }

}
