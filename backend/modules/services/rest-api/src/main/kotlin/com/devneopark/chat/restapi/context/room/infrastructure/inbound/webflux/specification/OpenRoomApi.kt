package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ApiResponseBase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/** 인증된 사용자가 채팅방을 개설하는 HTTP 계약이다. */
@Tag(name = "Room API")
interface OpenRoomApi {

    /** 현재 인증된 사용자를 호스트로 채팅방을 개설한다. */
    @PostMapping("/rooms")
    @Operation(summary = "채팅방 개설 API")
    @PreAuthorize("isAuthenticated()")
    suspend fun open(
        @RequestBody @Valid body: Request,
        authentication: Authentication
    ): ResponseEntity<Response>

    @Schema(name = "Open room api request body", description = "채팅방 개설에 필요한 정보")
    data class Request(

        @NotBlank
        @Size(max = 50)
        @Schema(description = "채팅방 제목")
        val title: String? = "",

        @Size(min = 1, max = 32)
        @Schema(description = "채팅방 비밀번호")
        val rawPassword: String? = null,

        @NotNull
        @Schema(description = "채팅방 정원")
        val capacity: Int? = null

    )

    @Schema(name = "Open room api response body", description = "채팅방 개설 결과")
    data class Response(

        val roomId: String

    ) : ApiResponseBase()

}
