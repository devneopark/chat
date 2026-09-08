package com.devneopark.chat.restapi.context.iam.application.port.inbound

/** 사용자 계정을 생성하는 응용 계층의 진입 계약이다. */
interface RegisterUserUseCase {

    /** 입력값을 검증하고 새로운 사용자를 등록한다. */
    suspend fun register(command: Command): Result

    /** 회원가입에 필요한 사용자 입력값이다. raw password는 해시 후 저장된다. */
    data class Command(

        val principal: String,

        val rawPassword: String,

        val displayName: String

    )

    /** 등록된 사용자의 식별자를 반환한다. */
    data class Result(

        val userId: String

    )

}
