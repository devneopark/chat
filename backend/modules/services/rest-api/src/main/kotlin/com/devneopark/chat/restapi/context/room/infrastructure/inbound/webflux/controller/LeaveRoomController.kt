package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.room.application.port.inbound.LeaveRoomUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.LeaveRoomApi
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

/** 인증된 사용자의 채팅방 퇴장 요청을 응용 계층으로 전달하는 WebFlux 컨트롤러다. */
@RestController
class LeaveRoomController(

    private val leaveRoomUseCase: LeaveRoomUseCase

) : LeaveRoomApi {

    override suspend fun leave(
        roomId: String,
        authentication: Authentication
    ): ResponseEntity<LeaveRoomApi.Response> {
        leaveRoomUseCase.leave(
            LeaveRoomUseCase.Command(
                roomId,
                authentication.name
            )
        )
        return ResponseEntity.ok(LeaveRoomApi.Response())
    }

}
