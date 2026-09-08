package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/** 인증되지 않은 사용자가 새 계정을 생성하는 HTTP 계약이다. */
@Tag(name = "User API")
interface RegisterUserApi {

    /** 인증된 사용자는 회원가입 API를 호출할 수 없다. */
    @PostMapping("/users")
    @Operation(summary = "회원가입 API")
    @PreAuthorize("isAnonymous()")
    suspend fun register(@RequestBody @Valid body: Request): ResponseEntity<Response>

    @Schema(name = "Register user api request body", description = "회원가입에 필요한 정보")
    data class Request(

        @NotBlank
        @Schema(description = "로그인 ID")
        val principal: String? = "",

        @NotBlank
        @Schema(description = "로그인 패스워드")
        val rawPassword: String? = "",

        @Size(min = 1, max = 50)
        @Schema(description = "회원 프로필 닉네임")
        val displayName: String? = null

    )

    @Schema(name = "Register user api response body", description = "회원 가입 결과")
    data class Response(

        @NotBlank
        @Schema(description = "생성된 회원 식별자")
        val userId: String

    ) : ApiResponseBase()

}
