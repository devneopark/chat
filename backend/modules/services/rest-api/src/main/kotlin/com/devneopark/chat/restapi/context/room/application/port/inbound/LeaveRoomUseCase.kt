package com.devneopark.chat.restapi.context.room.application.port.inbound

/** 사용자를 활성 채팅방에서 퇴장시키는 응용 계층의 진입 계약이다. */
interface LeaveRoomUseCase {

    /** 요청 사용자를 채팅방에서 퇴장시킨다. */
    suspend fun leave(command: Command)

    /** 채팅방 퇴장에 필요한 방 식별자와 사용자 식별자다. */
    data class Command(

        val roomId: String,

        val userId: String

    )

}
