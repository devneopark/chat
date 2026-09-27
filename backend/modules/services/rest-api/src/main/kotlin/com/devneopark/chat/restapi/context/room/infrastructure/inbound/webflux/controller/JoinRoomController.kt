package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.room.application.port.inbound.JoinRoomUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.JoinRoomApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 사용자의 채팅방 입장 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class JoinRoomController(

    private val joinRoomUseCase: JoinRoomUseCase

) : JoinRoomApi {

    override suspend fun join(
        roomId: String,
        body: JoinRoomApi.Request,
        authentication: Authentication
    ): ResponseEntity<JoinRoomApi.Response> {
        joinRoomUseCase.join(
            JoinRoomUseCase.Command(
                roomId,
                authentication.name,
                body.rawPassword
            )
        )
        return ResponseEntity.ok(JoinRoomApi.Response())
    }

}
