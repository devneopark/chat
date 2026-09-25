package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomCapacityUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.UpdateRoomCapacityApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 채팅방 호스트의 정원 변경 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class UpdateRoomCapacityController(

    private val updateRoomCapacityUseCase: UpdateRoomCapacityUseCase

) : UpdateRoomCapacityApi {

    override suspend fun update(
        roomId: String,
        body: UpdateRoomCapacityApi.Request,
        authentication: Authentication
    ): ResponseEntity<UpdateRoomCapacityApi.Response> {
        val command = UpdateRoomCapacityUseCase.Command(
            roomId,
            authentication.name,
            body.capacity!!
        )
        updateRoomCapacityUseCase.update(command)
        return ResponseEntity.ok(UpdateRoomCapacityApi.Response())
    }

}
