package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable

/** 인증된 사용자가 채팅방에서 퇴장하는 HTTP 계약이다. */
@Tag(name = "Room API")
interface LeaveRoomApi {

    /** 현재 인증된 사용자를 채팅방의 활성 참여자에서 제외한다. */
    @DeleteMapping("/rooms/{roomId}/participants")
    @Operation(summary = "채팅방 퇴장 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun leave(
        @PathVariable roomId: String,
        authentication: Authentication
    ): ResponseEntity<Response>

    /** 채팅방 퇴장 성공 결과를 담는 응답이다. */
    @Schema(name = "Leave room api response body", description = "채팅방 퇴장 결과")
    class Response : ApiResponseBase()

}
