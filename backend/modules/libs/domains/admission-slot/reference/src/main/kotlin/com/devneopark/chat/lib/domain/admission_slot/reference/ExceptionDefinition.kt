package com.devneopark.chat.lib.domain.admission_slot.reference

/**
 * 채팅방 슬롯 도메인에서 공유하는 예외 정의.
 *
 * 각 항목은 도메인 또는 응용 계층에서 예외를 생성할 때 사용할 안정적인 [code]와
 * 호출자에게 전달할 기본 [message]를 제공한다.
 *
 * @param code 채팅방 슬롯 도메인 예외를 식별하는 코드.
 * @param message 채팅방 슬롯 도메인 예외의 기본 메시지.
 */
enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    /**
     * 채팅방 ID가 제공되지 않은 경우.
     */
    ROOM_ID_REQUIRED("1-003-001", "Room ID is required."),

    /**
     * 채팅방 슬롯 번호가 허용된 범위가 아닌 경우.
     */
    INVALID_SLOT_NUMBER("1-003-002", "Admission slot number is invalid."),

    /**
     * 이미 점유된 채팅방 슬롯에 참가자를 할당하려는 경우.
     */
    ALREADY_OCCUPIED("1-003-003", "Admission slot is already occupied."),

    /**
     * 채팅방 슬롯의 점유자와 해제 요청의 참가자가 일치하지 않는 경우.
     */
    OCCUPANT_MISMATCH("1-003-004", "Admission slot occupant does not match."),

    ;

}
