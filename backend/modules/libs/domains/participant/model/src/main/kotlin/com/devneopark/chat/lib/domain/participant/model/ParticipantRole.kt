package com.devneopark.chat.lib.domain.participant.model

/**
 * 채팅방 참여자에게 부여할 수 있는 역할.
 */
enum class ParticipantRole {

    /**
     * 채팅방을 관리하는 호스트 역할.
     */
    HOST,

    /**
     * 채팅방에 참여한 일반 사용자 역할.
     */
    GUEST,

    ;

}
