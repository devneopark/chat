package com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux

object WebFluxErrorResponses {

    fun unexpectedError() = ExceptionResponse(
        "3-001-001",
        "Unexpected error."
    )

    fun notFound() = ExceptionResponse(
        "3-001-002",
        "Not found."
    )

    fun fieldBindingFailed(rejectedFields: List<String>) = FieldBindingExceptionResponse(
        "3-001-003",
        "Field binding failed.",
        rejectedFields
    )

    fun noRequestBody() = ExceptionResponse(
        "3-001-004",
        "Request body missing."
    )

    fun unsupportedMediaType() = ExceptionResponse(
        "3-001-005",
        "Unsupported media type."
    )

    fun conflict() = ExceptionResponse(
        "3-001-006",
        "Conflict."
    )

    fun serviceUnavailable() = ExceptionResponse(
        "3-001-007",
        "Service unavailable."
    )

    fun missingRequestValue() = ExceptionResponse(
        "3-001-008",
        "Request value missing."
    )

    fun unauthorized() = ExceptionResponse(
        "3-001-009",
        "Unauthorized."
    )

    fun forbidden() = ExceptionResponse(
        "3-001-010",
        "Forbidden."
    )

}
