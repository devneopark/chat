package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PutMapping
import java.time.Instant

/**
 * renewal credential 쿠키로 access token을 재발급하는 HTTP 계약이다.
 *
 * access token의 인증 여부와 관계없이 호출할 수 있으며, 재발급된 renewal credential은 쿠키로 교체된다.
 */
@Tag(name = "User API")
interface RefreshUserLoginApi {

    /** 인증 여부와 관계없이 refresh token 쿠키가 있으면 재발급을 시도한다. */
    @PutMapping("/authentications")
    @Operation(summary = "로그인 토큰 재발급")
    @PreAuthorize("permitAll()")
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
