package com.devneopark.chat.lib.domain.room.reference

/**
 * 채팅방 도메인 경계에서 공유하는 채팅방 식별자 계약.
 *
 * 채팅방 모델을 직접 의존하지 않는 모듈은 이 인터페이스를 통해 채팅방 식별자 값만 참조한다.
 */
interface RoomId {

    /**
     * 채팅방 식별자 값.
     */
    val value: String

}
