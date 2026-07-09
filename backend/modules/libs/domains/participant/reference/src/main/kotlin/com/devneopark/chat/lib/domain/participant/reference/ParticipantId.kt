package com.devneopark.chat.lib.domain.participant.reference

/**
 * 채팅방 참여자 도메인 경계에서 공유하는 참여자 식별자 계약.
 *
 * 채팅방 참여자 모델을 직접 의존하지 않는 모듈은 이 인터페이스를 통해 채팅방 참여자 식별자 값만 참조한다.
 * 참여자 식별자는 [roomId]와 [userId] 두 필드로 구성된 복합키이며,
 * 두 값을 함께 사용해야 하나의 채팅방 참여자를 식별할 수 있다.
 */
interface ParticipantId {

    /**
     * 복합키에서 채팅방을 식별하는 값.
     */
    val roomId: String

    /**
     * 복합키에서 채팅방 내부의 참여자가 어떤 유저인지 구분하는 값.
     */
    val userId: String

}
