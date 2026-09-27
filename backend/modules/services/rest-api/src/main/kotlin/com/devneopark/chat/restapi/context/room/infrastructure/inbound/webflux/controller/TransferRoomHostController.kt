package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.room.application.port.inbound.TransferRoomHostUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.TransferRoomHostApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 채팅방 호스트 변경 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class TransferRoomHostController(

    private val transferRoomHostUseCase: TransferRoomHostUseCase

) : TransferRoomHostApi {

    override suspend fun transfer(
        roomId: String,
        body: TransferRoomHostApi.Request,
        authentication: Authentication
    ): ResponseEntity<TransferRoomHostApi.Response> {
        val command = TransferRoomHostUseCase.Command(
            roomId,
            authentication.name,
            body.targetUserId!!
        )
        transferRoomHostUseCase.transfer(command)
        return ResponseEntity.ok(TransferRoomHostApi.Response())
    }

}
