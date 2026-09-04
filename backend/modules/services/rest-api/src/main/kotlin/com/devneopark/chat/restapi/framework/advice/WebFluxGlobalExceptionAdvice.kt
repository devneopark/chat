package com.devneopark.chat.restapi.framework.advice

import com.devneopark.chat.libs.shared.application.exception.ApplicationConflictException
import com.devneopark.chat.libs.shared.application.exception.ApplicationUnavailableException
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ExceptionDefinition as WebFluxExceptionDefinition
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.FieldBindingExceptionResponse
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.TransactionException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.MissingRequestValueException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.UnsupportedMediaTypeStatusException

private val logger = LoggerFactory.getLogger(WebFluxGlobalExceptionAdvice::class.java)

@RestControllerAdvice
class WebFluxGlobalExceptionAdvice {

    @ExceptionHandler(UnsupportedMediaTypeStatusException::class)
    suspend fun on(cause: UnsupportedMediaTypeStatusException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE
        val exceptionDefinition = WebFluxExceptionDefinition.UNSUPPORTED_MEDIA_TYPE
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
        val exceptionDefinition = WebFluxExceptionDefinition.NO_REQUEST_BODY
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
        val exceptionDefinition = WebFluxExceptionDefinition.FIELD_BINDING_FAILED
        val rejectedFields = cause.fieldErrors.map { it.field }
        val response = FieldBindingExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message,
            rejectedFields
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(MissingRequestValueException::class)
    suspend fun on(cause: MissingRequestValueException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.BAD_REQUEST
        val exceptionDefinition = WebFluxExceptionDefinition.MISSING_REQUEST_VALUE
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(DomainRuleViolationException::class)
    suspend fun on(cause: DomainRuleViolationException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.BAD_REQUEST
        val response = ExceptionResponse(cause.code, cause.message)
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(IamContextException::class)
    suspend fun on(cause: IamContextException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.BAD_REQUEST
        val response = ExceptionResponse(cause.code, cause.message)
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(ApplicationConflictException::class)
    suspend fun on(cause: ApplicationConflictException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.CONFLICT
        val response = ExceptionResponse(cause.code, cause.message)
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(DuplicateKeyException::class)
    suspend fun on(cause: DuplicateKeyException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.CONFLICT
        val exceptionDefinition = WebFluxExceptionDefinition.CONFLICT
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(ApplicationUnavailableException::class)
    suspend fun on(cause: ApplicationUnavailableException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.SERVICE_UNAVAILABLE
        val response = ExceptionResponse(cause.code, cause.message)
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(DataAccessException::class)
    suspend fun on(cause: DataAccessException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.SERVICE_UNAVAILABLE
        val exceptionDefinition = WebFluxExceptionDefinition.SERVICE_UNAVAILABLE
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(TransactionException::class)
    suspend fun on(cause: TransactionException): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.SERVICE_UNAVAILABLE
        val exceptionDefinition = WebFluxExceptionDefinition.SERVICE_UNAVAILABLE
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    suspend fun on(): ResponseEntity<ExceptionResponse> {
        val httpStatus = HttpStatus.NOT_FOUND
        val exceptionDefinition = WebFluxExceptionDefinition.NOT_FOUND
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        return ResponseEntity.status(httpStatus).body(response)
    }

    @ExceptionHandler(ResponseStatusException::class)
    suspend fun on(cause: ResponseStatusException): ResponseEntity<ExceptionResponse> {
        val httpStatus = cause.body.status
        val exceptionDefinition = WebFluxExceptionDefinition.UNEXPECTED_ERROR
        val response = ExceptionResponse(
            exceptionDefinition.code,
            exceptionDefinition.message
        )
        logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, cause)
        return ResponseEntity.status(httpStatus).body(response)
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
