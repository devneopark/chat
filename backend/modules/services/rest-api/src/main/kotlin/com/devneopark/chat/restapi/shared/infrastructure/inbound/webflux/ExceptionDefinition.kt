package com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux

enum class ExceptionDefinition(

    val code: String,

    val message: String

) {

    UNEXPECTED_ERROR("3-001-001", "Unexpected error."),

    NOT_FOUND("3-001-002", "Not found."),

    FIELD_BINDING_FAILED("3-001-003", "Field binding failed."),

    NO_REQUEST_BODY("3-001-004", "Request body missing."),

    UNSUPPORTED_MEDIA_TYPE("3-001-005", "Unsupported media type."),

    CONFLICT("3-001-006", "Conflict."),

    SERVICE_UNAVAILABLE("3-001-007", "Service unavailable."),

    MISSING_REQUEST_VALUE("3-001-008", "Request value missing."),

}
