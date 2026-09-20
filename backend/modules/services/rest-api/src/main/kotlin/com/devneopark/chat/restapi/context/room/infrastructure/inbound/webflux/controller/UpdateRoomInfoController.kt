package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomInfoUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.UpdateRoomInfoApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 채팅방 호스트의 정보 변경 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class UpdateRoomInfoController(

    private val updateRoomInfoUseCase: UpdateRoomInfoUseCase

) : UpdateRoomInfoApi {

    override suspend fun update(
        roomId: String,
        body: UpdateRoomInfoApi.Request,
        authentication: Authentication
    ): ResponseEntity<UpdateRoomInfoApi.Response> {
        val command = UpdateRoomInfoUseCase.Command(
            roomId,
            authentication.name,
            body.title!!,
            body.rawPassword
        )
        updateRoomInfoUseCase.update(command)
        return ResponseEntity.ok(UpdateRoomInfoApi.Response())
    }

}
