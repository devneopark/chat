package com.devneopark.chat.restapi.context.room.application.port.inbound

/** 사용자를 활성 채팅방의 참여자로 등록하는 응용 계층의 진입 계약이다. */
interface JoinRoomUseCase {

    /** 요청 사용자를 빈 슬롯에 배정해 채팅방에 입장시킨다. */
    suspend fun join(command: Command)

    /** 채팅방 입장에 필요한 방 식별자, 사용자 식별자, 비밀번호다. */
    data class Command(

        val roomId: String,

        val userId: String,

        val rawPassword: String?

    )

}
