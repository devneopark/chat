package com.devneopark.chat.lib.domain.chat_message.reference

/**
 * 채팅 메시지 도메인에서 공유하는 예외 정의.
 *
 * 각 항목은 도메인 또는 응용 계층에서 예외를 생성할 때 사용할 안정적인 [code]와
 * 호출자에게 전달할 기본 [message]를 제공한다.
 *
 * @param code 채팅 메시지 도메인 예외를 식별하는 코드.
 * @param message 채팅 메시지 도메인 예외의 기본 메시지.
 */
enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    /**
     * 채팅 메시지 ID가 제공되지 않은 경우.
     */
    CHAT_MESSAGE_ID_REQUIRED("1-005-001", "Chat message ID is required."),

    /**
     * 채팅 메시지 본문이 제공되지 않은 경우.
     */
    MESSAGE_PAYLOAD_REQUIRED("1-005-002", "Message content is required."),

    /**
     * 다른 참여자 언급시 언급할 사용자의 표시 이름 스냅샷이 제공되지 않은 경우.
     */
    DISPLAY_NAME_SNAPSHOT_REQUIRED("1-005-003", "Display name snapshot is required."),

    /**
     * 멘션 시작 인덱스가 음수인 경우.
     */
    INVALID_MENTION_START_INDEX_NUMBER("1-005-004", "Mention start index is invalid."),

    /**
     * 멘션 종료 인덱스가 음수인 경우.
     */
    INVALID_MENTION_END_INDEX_NUMBER("1-005-005", "Mention end index is invalid."),

    /**
     * 멘션 시작 인덱스가 종료 인덱스보다 크거나 같은 경우 또는
     * 멘션 범위가 메시지 본문을 벗어난 경우.
     */
    INVALID_MENTION_RANGE("1-005-006", "Mention range is invalid."),

    /**
     * 하나의 메시지 본문에서 둘 이상의 멘션 범위가 겹치는 경우.
     */
    OVERLAPPING_MENTION_RANGES("1-005-007", "Mention ranges must not overlap."),

    /**
     * 채팅 메시지가 자기 자신을 스레드 루트로 지정한 경우.
     */
    SELF_THREAD_ROOT_NOT_ALLOWED(
        "1-005-008",
        "Chat message cannot be its own thread root."
    ),

    /**
     * 채팅 메시지가 자기 자신을 답장 대상으로 지정한 경우.
     */
    SELF_REPLY_NOT_ALLOWED("1-005-009", "Chat message cannot reply to itself."),

    ;

}
