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
     * 참여자 유저 ID가 제공되지 않은 경우.
     */
    PARTICIPANT_USER_ID_REQUIRED("1-002-002", "Participant user ID is required."),

    /**
     * 채팅방 비밀번호가 필요한데 제공되지 않은 경우.
     */
    ROOM_PASSWORD_REQUIRED("1-002-003", "Room password is required."),

    /**
     * 채팅방 title 값이 허용된 형식이 아닌 경우.
     */
    INVALID_ROOM_TITLE("1-002-004", "Room title is invalid."),

    /**
     * 채팅방 비밀번호가 허용된 형식이 아닌 경우.
     */
    INVALID_ROOM_PASSWORD("1-002-005", "Room password is invalid."),

    /**
     * 채팅방 정원이 허용된 범위가 아닌 경우.
     */
    INVALID_ROOM_CAPACITY("1-002-006", "Room capacity is invalid."),

    /**
     * 채팅방 방장 권한이 필요한 작업을 시도한 경우.
     */
    ROOM_HOST_PERMISSION_REQUIRED("1-002-007", "Room host permission is required."),

    /**
     * 이미 참여중인 채팅방인 경우.
     */
    ALREADY_JOINED_ROOM("1-002-008", "Participant has already joined the room."),

    /**
     * 채팅방 정원을 초과해 참여하려는 경우.
     */
    ROOM_CAPACITY_EXCEEDED("1-002-009", "Room capacity has been exceeded."),

    /**
     * 채팅방 정원이 현재 참여자 수보다 작게 설정되는 경우.
     */
    ROOM_CAPACITY_BELOW_PARTICIPANT_COUNT(
        "1-002-010",
        "Room capacity cannot be less than participant count."
    ),

    /**
     * 채팅방 비밀번호가 일치하지 않는 경우.
     */
    INCORRECT_ROOM_PASSWORD("1-002-011", "Room password is incorrect."),

    /**
     * 채팅방 ID에 해당하는 채팅방를 찾을 수 없는 경우.
     */
    ROOM_NOT_FOUND("1-002-012", "Room was not found."),

    /**
     * 채팅방 참여자를 찾을 수 없는 경우.
     */
    PARTICIPANT_NOT_FOUND("1-002-013", "Room participant was not found."),

    ;

}
