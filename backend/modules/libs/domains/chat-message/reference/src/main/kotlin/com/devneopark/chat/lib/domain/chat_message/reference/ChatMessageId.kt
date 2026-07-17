package com.devneopark.chat.lib.domain.chat_message.reference

/**
 * 채팅 메시지 도메인 경계에서 공유하는 채팅 메시지 식별자 계약.
 *
 * 채팅 메시지 모델을 직접 의존하지 않는 모듈은 이 인터페이스를 통해 채팅 메시지 식별자 값만 참조한다.
 */
interface ChatMessageId {

    /**
     * 채팅 메시지 식별자 값.
     */
    val value: String

}
