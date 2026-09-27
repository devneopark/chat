package com.devneopark.chat.restapi.context.room.application.port.inbound

/** 활성 채팅방의 정보를 변경하는 응용 계층의 진입 계약이다. */
interface UpdateRoomInfoUseCase {

    /** 채팅방 제목과 비밀번호를 변경한다. */
    suspend fun update(command: Command)

    /** 채팅방 정보 변경에 필요한 방 식별자, 요청자 식별자, 변경 정보다. */
    data class Command(

        /** 변경할 채팅방 식별자다. */
        val roomId: String,

        /** 변경을 요청한 사용자의 식별자다. */
        val hostUserId: String,

        /** 변경할 채팅방 제목이다. */
        val title: String,

        /** 변경할 원문 비밀번호다. `null`이면 비밀번호를 제거한다. */
        val rawPassword: String?

    )

}
