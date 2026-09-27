package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/** 인증된 사용자가 채팅방에 입장하는 HTTP 계약이다. */
@Tag(name = "Room API")
interface JoinRoomApi {

    /** 현재 인증된 사용자를 채팅방의 게스트 참여자로 등록한다. */
    @PostMapping("/rooms/{roomId}/participants")
    @Operation(summary = "채팅방 입장 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun join(
        @PathVariable roomId: String,
        @RequestBody @Valid body: Request,
        authentication: Authentication
    ): ResponseEntity<Response>

    /** 보호된 채팅방 입장에 사용할 선택적 비밀번호다. */
    @Schema(name = "Join room api request body", description = "채팅방 입장에 필요한 정보")
    data class Request(

        @Size(min = 1, max = 32)
        @Schema(description = "채팅방 비밀번호")
        val rawPassword: String? = null

    )

    /** 채팅방 입장 성공 결과를 담는 응답이다. */
    @Schema(name = "Join room api response body", description = "채팅방 입장 결과")
    class Response : ApiResponseBase()

}
