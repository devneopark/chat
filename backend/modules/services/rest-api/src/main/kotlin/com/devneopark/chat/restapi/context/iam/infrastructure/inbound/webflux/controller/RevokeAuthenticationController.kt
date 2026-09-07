package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.iam.application.port.inbound.RevokeAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification.RevokeAuthenticationApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

@RestController
class RevokeAuthenticationController(

    private val revokeAuthenticationUseCase: RevokeAuthenticationUseCase

) : RevokeAuthenticationApi {

    override suspend fun revoke(authentication: Authentication): ResponseEntity<RevokeAuthenticationApi.Response> {
        val jti = authentication.credentials as String
        val command = RevokeAuthenticationUseCase.Command(jti)
        revokeAuthenticationUseCase.revoke(command)
        return ResponseEntity.ok(RevokeAuthenticationApi.Response())
    }

}
