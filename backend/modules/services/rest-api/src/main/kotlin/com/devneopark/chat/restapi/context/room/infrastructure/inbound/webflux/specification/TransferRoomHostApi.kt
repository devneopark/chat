package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/** 인증된 채팅방 호스트가 호스트 권한을 다른 참여자에게 이전하는 HTTP 계약이다. */
@Tag(name = "Room API")
interface TransferRoomHostApi {

    /** 현재 인증된 사용자의 호스트 권한을 대상 참여자에게 이전한다. */
    @PostMapping("/rooms/{roomId}/host")
    @Operation(summary = "채팅방 호스트 변경 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun transfer(
        @PathVariable roomId: String,
        @RequestBody @Valid body: Request,
        authentication: Authentication
    ): ResponseEntity<Response>

    /** 호스트 권한을 이전할 대상 참여자의 식별자다. */
    @Schema(name = "Transfer room host api request body", description = "변경할 채팅방 호스트 정보")
    data class Request(

        @NotBlank
        @Schema(description = "호스트 권한을 이전할 참여자의 사용자 식별자")
        val targetUserId: String? = ""

    )

    /** 채팅방 호스트 변경 성공 결과를 담는 응답이다. */
    @Schema(name = "Transfer room host api response body", description = "채팅방 호스트 변경 결과")
    class Response : ApiResponseBase()

}
