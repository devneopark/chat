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
     * 사용자 principal 값이 이미 사용 중인 경우.
     */
    USER_PRINCIPAL_DUPLICATED("1-001-003", "User principal is already in use."),

    /**
     * 사용자 비밀번호가 허용된 형식이 아닌 경우.
     */
    INVALID_USER_PASSWORD("1-001-004", "User password is invalid."),

    /**
     * 기존 비밀번호와 동일한 인증 정보를 다시 사용하려는 경우.
     */
    USER_PASSWORD_REUSED("1-001-005", "User password cannot be reused."),

    /**
     * 사용자 표시 이름이 허용된 형식이 아닌 경우.
     */
    INVALID_USER_DISPLAY_NAME("1-001-006", "User display name is invalid."),

    /**
     * 사용자 ID에 해당하는 사용자를 찾을 수 없는 경우.
     */
    USER_NOT_FOUND("1-001-007", "User was not found."),

    /**
     * 사용자 프로필이 제공되지 않은 경우.
     */
    USER_PROFILE_REQUIRED("1-001-008", "User profile is required."),

    /**
     * 사용자 인증 정보가 제공되지 않은 경우.
     */
    USER_CREDENTIAL_REQUIRED("1-001-009", "User credential is required."),

    ;

}
