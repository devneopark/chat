package com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

/** 인증된 사용자의 프로필을 수정하는 HTTP 계약이다. */
@Tag(name = "User API")
interface UpdateUserProfileApi {

    /** 현재 인증된 사용자의 프로필을 수정한다. */
    @PutMapping("/users/profiles")
    @Operation(summary = "회원 프로필 수정 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun updateProfile(
        @RequestBody @Valid body: Request,
        authentication: Authentication
    ): ResponseEntity<Response>

    /** 프로필 수정 요청에서 전달하는 사용자 표시 이름이다. */
    @Schema(name = "Update user profile api request body", description = "수정할 회원 프로필 정보")
    data class Request(

        /** 비어 있지 않고 허용된 길이 범위에 있는 표시 이름이다. */
        @NotBlank
        @Size(min = 1, max = 20)
        @Schema(description = "회원 프로필 닉네임")
        val displayName: String? = ""

    )

    /** 프로필 수정 성공 결과를 담는 응답이다. */
    @Schema(name = "Update user profile api response body", description = "회원 프로필 수정 결과")
    class Response : ApiResponseBase()

}
