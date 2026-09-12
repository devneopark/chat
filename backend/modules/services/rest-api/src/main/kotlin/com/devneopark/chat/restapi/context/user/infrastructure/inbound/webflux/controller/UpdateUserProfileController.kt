package com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.user.application.port.inbound.UpdateUserProfileUseCase
import com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.specification.UpdateUserProfileApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 사용자의 프로필 수정 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class UpdateUserProfileController(

    private val updateUserProfileUseCase: UpdateUserProfileUseCase

) : UpdateUserProfileApi {

    override suspend fun updateProfile(
        body: UpdateUserProfileApi.Request,
        authentication: Authentication
    ): ResponseEntity<UpdateUserProfileApi.Response> {
        val command = UpdateUserProfileUseCase.Command(
            authentication.name,
            body.displayName!!
        )
        updateUserProfileUseCase.update(command)
        return ResponseEntity.ok(UpdateUserProfileApi.Response())
    }

}
