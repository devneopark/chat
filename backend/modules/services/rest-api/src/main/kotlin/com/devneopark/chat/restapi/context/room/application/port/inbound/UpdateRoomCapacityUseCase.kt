package com.devneopark.chat.restapi.context.room.application.port.inbound

/** 활성 채팅방의 정원을 변경하는 응용 계층의 진입 계약이다. */
interface UpdateRoomCapacityUseCase {

    /** 채팅방의 정원을 변경한다. */
    suspend fun update(command: Command)

    /** 채팅방 정원 변경에 필요한 방 식별자, 호스트 식별자, 목표 정원이다. */
    data class Command(

        /** 변경할 채팅방 식별자다. */
        val roomId: String,

        /** 변경을 요청한 호스트의 사용자 식별자다. */
        val hostUserId: String,

        /** 변경할 목표 정원이다. */
        val newCapacity: Int

    )

}
