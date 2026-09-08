package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping

/**
 * 현재 인증된 access credential을 폐기하는 HTTP 계약이다.
 * 폐기는 요청의 사용자 식별자가 아니라 인증 필터가 설정한 access credential의 JTI를 기준으로 수행된다.
 */
@Tag(name = "User API")
interface RevokeAuthenticationApi {

    /** 인증된 access credential의 JTI에 해당하는 인증정보를 삭제한다. */
    @DeleteMapping("/authentications")
    @Operation(summary = "로그아웃 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun revoke(authentication: Authentication): ResponseEntity<Response>

    @Schema(name = "Revoke authentication api response body", description = "로그아웃 결과")
    class Response : ApiResponseBase()

}
