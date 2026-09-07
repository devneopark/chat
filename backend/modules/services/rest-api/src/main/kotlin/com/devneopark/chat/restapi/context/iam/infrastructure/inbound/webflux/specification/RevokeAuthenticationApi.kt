package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping

@Tag(name = "User API")
interface RevokeAuthenticationApi {

    @DeleteMapping("/authentications")
    @Operation(summary = "로그아웃 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun revoke(authentication: Authentication): ResponseEntity<Response>

    @Schema(name = "Revoke authentication api response body", description = "로그아웃 결과")
    class Response : ApiResponseBase()

}
