package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

/** 인증된 채팅방 호스트가 채팅방 정원을 변경하는 HTTP 계약이다. */
@Tag(name = "Room API")
interface UpdateRoomCapacityApi {

    /** 현재 인증된 사용자가 호스트인 채팅방의 정원을 변경한다. */
    @PutMapping("/rooms/{roomId}/capacity")
    @Operation(summary = "채팅방 정원 변경 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun update(
        @PathVariable roomId: String,
        @RequestBody @Valid body: Request,
        authentication: Authentication
    ): ResponseEntity<Response>

    @Schema(name = "Update room capacity api request body", description = "변경할 채팅방 정원")
    data class Request(

        @NotNull
        @Min(2)
        @Schema(description = "채팅방 정원")
        val capacity: Int? = null

    )

    @Schema(name = "Update room capacity api response body", description = "채팅방 정원 변경 결과")
    class Response : ApiResponseBase()

}
