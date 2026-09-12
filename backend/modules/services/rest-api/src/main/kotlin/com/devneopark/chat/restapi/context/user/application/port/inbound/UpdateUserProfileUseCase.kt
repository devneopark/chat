package com.devneopark.chat.restapi.context.user.application.port.inbound

/** 인증된 사용자의 프로필을 수정하는 응용 계층의 진입 계약이다. */
interface UpdateUserProfileUseCase {

    /** 사용자의 프로필 정보를 변경한다. */
    suspend fun update(command: Command)

    /** 프로필 수정에 필요한 사용자 식별자와 표시 이름이다. */
    data class Command(

        /** 수정할 사용자의 식별자다. */
        val userId: String,

        /** 변경할 사용자의 표시 이름이다. */
        val displayName: String

    )

}
