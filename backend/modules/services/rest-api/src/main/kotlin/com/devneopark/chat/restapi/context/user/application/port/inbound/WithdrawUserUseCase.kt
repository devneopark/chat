package com.devneopark.chat.restapi.context.user.application.port.inbound

/** 인증된 사용자의 탈퇴를 처리하는 응용 계층의 진입 계약이다. */
interface WithdrawUserUseCase {

    /** 활성 채팅방 참여 여부를 확인한 뒤 사용자를 탈퇴 처리한다. */
    suspend fun withdraw(command: Command)

    /** 사용자 탈퇴에 필요한 식별자다. */
    data class Command(

        /** 탈퇴할 사용자의 식별자다. */
        val userId: String

    )

}
