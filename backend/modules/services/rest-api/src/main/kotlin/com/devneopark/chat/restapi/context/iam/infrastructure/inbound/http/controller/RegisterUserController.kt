package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.controller

import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RegisterUserUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.specification.RegisterUserApi
import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.ExceptionResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = LoggerFactory.getLogger(RegisterUserController::class.java)

@RestController
class RegisterUserController(

    private val registerUserUseCase: RegisterUserUseCase

) : RegisterUserApi {

    override suspend fun register(body: RegisterUserApi.Request): ResponseEntity<RegisterUserApi.Response> {
        val displayName = body.displayName ?: body.principal
        val command = RegisterUserUseCase.Command(
            body.principal!!,
            body.rawPassword!!,
            displayName!!
        )
        val result = registerUserUseCase.register(command)
        val response = RegisterUserApi.Response(result.userId)
        logger.trace("User registration succeed. userId={}", response.userId)
        return ResponseEntity.ok(response)
    }

    @RestControllerAdvice(assignableTypes = [ RegisterUserController::class ])
    class Advice {

        @ExceptionHandler(DomainRuleViolationException::class)
        suspend fun on(exception: DomainRuleViolationException): ResponseEntity<ExceptionResponse> {
            val httpStatus = HttpStatus.BAD_REQUEST
            val response = ExceptionResponse(exception.code, exception.message)
            logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, exception)
            return ResponseEntity.status(httpStatus).body(response)
        }

        @ExceptionHandler(IamContextException::class)
        suspend fun on(exception: IamContextException): ResponseEntity<ExceptionResponse> {
            val httpStatus = HttpStatus.BAD_REQUEST
            val response = ExceptionResponse(exception.code, exception.message)
            logger.debug("API failed with {}. exception-code={} message={}", httpStatus, response.code, response.message, exception)
            return ResponseEntity.status(httpStatus).body(response)
        }

    }

}