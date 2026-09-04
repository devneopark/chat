package com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.UnsupportedMediaTypeStatusException

private val logger = LoggerFactory.getLogger(RootApiExceptionAdvice::class.java)

@RestControllerAdvice
class RootApiExceptionAdvice {

    @ExceptionHandler(UnsupportedMediaTypeStatusException::class)
    suspend fun on(cause: UnsupportedMediaTypeStatusException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE
        val exceptionDefinition = ExceptionDefinition.UNSUPPORTED_MEDIA_TYPE
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(ServerWebInputException::class)
    suspend fun on(cause: ServerWebInputException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.BAD_REQUEST
        val exceptionDefinition = ExceptionDefinition.NO_REQUEST_BODY
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(WebExchangeBindException::class)
    suspend fun on(cause: WebExchangeBindException): ResponseEntity<FieldBindingExceptionResponse> {
        val httpStatus = HttpStatus.BAD_REQUEST
        val exceptionDefinition = ExceptionDefinition.FIELD_BINDING_FAILED
        val rejectedFields = cause.fieldErrors.map { it.field }
        val response = FieldBindingExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message,
            rejectedFields
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    suspend fun on(): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.NOT_FOUND
        val exceptionDefinition = ExceptionDefinition.NOT_FOUND
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(ResponseStatusException::class)
    suspend fun on(cause: ResponseStatusException): ResponseEntity<ExceptionResponse> {
        val httpStatus = cause.body.status
        val exceptionDefinition = ExceptionDefinition.UNEXPECTED_ERROR
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).build()
    }

    @ExceptionHandler(ExceptionBase::class)
    suspend fun on(cause: ExceptionBase): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val response = ExceptionResponse(cause.code, cause.message)
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(Throwable::class)
    suspend fun on(cause: Throwable): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val response = ExceptionResponse("9-999-999", "Unexpected error.")
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

}