package com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.user.application.port.inbound.WithdrawUserUseCase
import com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.specification.WithdrawUserApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 사용자의 회원탈퇴 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class WithdrawUserController(

    private val withdrawUserUseCase: WithdrawUserUseCase

) : WithdrawUserApi {

    override suspend fun withdraw(authentication: Authentication): ResponseEntity<WithdrawUserApi.Response> {
        val command = WithdrawUserUseCase.Command(authentication.name)
        withdrawUserUseCase.withdraw(command)
        return ResponseEntity.ok(WithdrawUserApi.Response())
    }

}
