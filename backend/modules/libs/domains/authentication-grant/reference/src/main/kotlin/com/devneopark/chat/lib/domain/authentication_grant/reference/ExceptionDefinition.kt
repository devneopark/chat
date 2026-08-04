package com.devneopark.chat.lib.domain.authentication_grant.reference

/**
 * Authentication Grant 도메인이 공유하는 예외 정의.
 */
enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    AUTHENTICATION_GRANT_ID_REQUIRED("1-006-001", "Authentication grant ID is required."),

    ACCESS_CREDENTIAL_ID_REQUIRED("1-006-005", "Access credential ID is required."),

    RENEWAL_CREDENTIAL_ID_REQUIRED("1-006-004", "Renewal credential ID is required."),

    CREDENTIAL_ISSUED_AT_INVALID("1-006-017", "Credential issued-at is invalid."),

    INVALID_ACCESS_CREDENTIAL_EXPIRATION("1-006-018", "Access credential expiration is invalid."),

    INVALID_RENEWAL_CREDENTIAL_EXPIRATION("1-006-020", "Renewal credential expiration is invalid."),

    RENEWAL_CREDENTIAL_NOT_USABLE("1-006-010", "Renewal credential is not usable."),

    RENEWAL_CREDENTIAL_MISMATCH("1-006-011", "Renewal credential does not match the current credential.")

    ;

}
