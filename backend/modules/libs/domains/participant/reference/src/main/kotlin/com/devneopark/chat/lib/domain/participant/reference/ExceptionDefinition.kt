package com.devneopark.chat.lib.domain.participant.reference

/**
 * 채팅방 참여자 도메인에서 공유하는 예외 정의.
 *
 * 각 항목은 도메인 또는 응용 계층에서 예외를 생성할 때 사용할 안정적인 [code]와
 * 호출자에게 전달할 기본 [message]를 제공한다.
 *
 * @param code 채팅방 참여자 도메인 예외를 식별하는 코드.
 * @param message 채팅방 참여자 도메인 예외의 기본 메시지.
 */
enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    /**
     * 참여자 ID가 제공되지 않은 경우.
     */
    PARTICIPANT_ID_REQUIRED("1-004-001", "Participant ID is required."),

    ;

}
