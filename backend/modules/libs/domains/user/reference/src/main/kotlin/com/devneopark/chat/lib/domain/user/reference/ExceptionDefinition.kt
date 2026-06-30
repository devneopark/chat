package com.devneopark.chat.lib.domain.user.reference

/**
 * 사용자 도메인에서 공유하는 예외 정의.
 *
 * 각 항목은 도메인 또는 응용 계층에서 예외를 생성할 때 사용할 안정적인 [code]와
 * 호출자에게 전달할 기본 [message]를 제공한다.
 *
 * @param code 사용자 도메인 예외를 식별하는 코드.
 * @param message 사용자 도메인 예외의 기본 메시지.
 */
enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    /**
     * 사용자 ID가 제공되지 않은 경우.
     */
    USER_ID_REQUIRED("1-001-001", "User ID is required."),

    /**
     * 사용자 principal 값이 허용된 형식이 아닌 경우.
     */
    INVALID_USER_PRINCIPAL("1-001-002", "User principal is invalid."),

    /**
     * 사용자 비밀번호가 허용된 형식이 아닌 경우.
     */
    INVALID_USER_PASSWORD("1-001-003", "User password is invalid."),

    /**
     * 사용자 표시 이름이 허용된 형식이 아닌 경우.
     */
    INVALID_USER_DISPLAY_NAME("1-001-004", "User display name is invalid."),

    ;

}
