package com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping

/** 인증된 사용자의 회원탈퇴 HTTP 계약이다. */
@Tag(name = "User API")
interface WithdrawUserApi {

    /** 현재 인증된 사용자의 계정을 탈퇴 처리한다. */
    @DeleteMapping("/users")
    @Operation(summary = "회원탈퇴 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun withdraw(authentication: Authentication): ResponseEntity<Response>

    /** 회원탈퇴 성공 결과를 담는 응답이다. */
    @Schema(name = "Withdraw user api response body", description = "회원탈퇴 결과")
    class Response : ApiResponseBase()

}
