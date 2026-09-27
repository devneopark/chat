package com.devneopark.chat.restapi.context.room.application.port.inbound

/** 활성 채팅방의 호스트를 다른 활성 참여자에게 변경하는 응용 계층의 진입 계약이다. */
interface TransferRoomHostUseCase {

    /** 현재 호스트의 권한을 대상 참여자에게 이전한다. */
    suspend fun transfer(command: Command)

    /** 호스트 변경에 필요한 방 식별자, 요청자 식별자, 대상 참여자 식별자다. */
    data class Command(

        val roomId: String,

        val requesterUserId: String,

        val targetUserId: String

    )

}
