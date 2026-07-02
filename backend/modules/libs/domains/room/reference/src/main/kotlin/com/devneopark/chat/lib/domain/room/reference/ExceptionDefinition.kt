package com.devneopark.chat.lib.domain.room.reference

/**
 * 채팅방 도메인에서 공유하는 예외 정의.
 *
 * 각 항목은 도메인 또는 응용 계층에서 예외를 생성할 때 사용할 안정적인 [code]와
 * 호출자에게 전달할 기본 [message]를 제공한다.
 *
 * @param code 채팅방 도메인 예외를 식별하는 코드.
 * @param message 채팅방 도메인 예외의 기본 메시지.
 */
enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    /**
     * 채팅방 ID가 제공되지 않은 경우.
     */
    ROOM_ID_REQUIRED("1-002-001", "Room ID is required."),

    /**
     * 호스트 유저 ID가 제공되지 않은 경우.
     */
    HOST_USER_ID_REQUIRED("1-002-002", "Host user ID is required."),

    /**
     * 채팅방 title 값이 허용된 형식이 아닌 경우.
     */
    INVALID_ROOM_TITLE("1-002-003", "Room title is invalid."),

    /**
     * 채팅방 비밀번호가 허용된 형식이 아닌 경우.
     */
    INVALID_ROOM_PASSWORD("1-002-004", "Room password is invalid.")

    ;

}
