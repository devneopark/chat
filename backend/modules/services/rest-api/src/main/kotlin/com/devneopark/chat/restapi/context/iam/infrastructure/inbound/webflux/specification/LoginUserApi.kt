package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.Instant

@Tag(name = "User API")
interface LoginUserApi {

    @PostMapping("/authentications")
    @Operation(summary = "로그인 토큰 발급 API")
    @PreAuthorize("isAnonymous()")
    suspend fun login(@RequestBody @Valid body: Request): ResponseEntity<Response>

    @Schema(name = "Login user api request body", description = "로그인에 필요한 정보")
    data class Request(

        @NotBlank
        @Schema(description = "로그인 ID")
        val principal: String? = "",

        @NotBlank
        @Schema(description = "로그인 패스워드")
        val rawPassword: String? = "",

    )

    @Schema(name = "Login user api response body", description = "로그인 결과")
    data class Response(

        @Schema(description = "엑세스 토큰")
        val accessToken: String,

        @Schema(description = "엑세스 토큰 만료시점")
        val expiresAt: Instant

    ) : ApiResponseBase()

}
