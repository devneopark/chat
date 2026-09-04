package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.iam.application.port.inbound.RegisterUserUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification.RegisterUserApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

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

}
