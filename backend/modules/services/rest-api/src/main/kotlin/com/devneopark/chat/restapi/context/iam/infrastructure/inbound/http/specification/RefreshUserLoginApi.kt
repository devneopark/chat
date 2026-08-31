package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PutMapping
import java.time.Instant

@Tag(name = "User API")
interface RefreshUserLoginApi {

    @PutMapping("/authentications")
    @Operation(summary = "로그인 토큰 재발급")
    suspend fun refresh(
        @CookieValue(name = $$"${chat.infrastructure.auth.cookie.name}")
        refreshToken: String
    ): ResponseEntity<Response>

    @Schema(name = "Refresh user login api response body", description = "로그인 재발급 결과")
    data class Response(

        @Schema(description = "엑세스 토큰")
        val accessToken: String,

        @Schema(description = "엑세스 토큰 만료시점")
        val expiresAt: Instant

    ) : ApiResponseBase()

}