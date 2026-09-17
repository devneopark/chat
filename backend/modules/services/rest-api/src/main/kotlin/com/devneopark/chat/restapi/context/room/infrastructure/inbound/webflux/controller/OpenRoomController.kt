package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.room.application.port.inbound.OpenRoomUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.OpenRoomApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 사용자의 채팅방 개설 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class OpenRoomController(

    private val openRoomUseCase: OpenRoomUseCase

) : OpenRoomApi {

    override suspend fun open(
        body: OpenRoomApi.Request,
        authentication: Authentication
    ): ResponseEntity<OpenRoomApi.Response> {
        val command = OpenRoomUseCase.Command(
            body.title!!,
            body.rawPassword,
            body.capacity!!,
            authentication.name
        )
        val result = openRoomUseCase.open(command)
        return ResponseEntity.ok(OpenRoomApi.Response(result.roomId))
    }

}
