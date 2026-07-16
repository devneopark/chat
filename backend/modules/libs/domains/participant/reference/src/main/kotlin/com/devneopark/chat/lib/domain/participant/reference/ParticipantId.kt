package com.devneopark.chat.lib.domain.participant.reference

/**
 * 채팅방 참여자 도메인 경계에서 공유하는 참여자 식별자 계약.
 *
 * 채팅방 참여자 모델을 직접 의존하지 않는 모듈은 이 인터페이스를 통해 채팅방 참여자 식별자 값만 참조한다.
 */
interface ParticipantId {

    /**
     * 참여자 식별자 값.
     */
    val value: String

}
