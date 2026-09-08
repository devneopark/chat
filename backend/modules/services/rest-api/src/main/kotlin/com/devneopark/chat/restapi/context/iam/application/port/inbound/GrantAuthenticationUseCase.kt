package com.devneopark.chat.restapi.context.iam.application.port.inbound

import kotlin.time.Instant

/**
 * 사용자 자격증명을 확인하고 access·renewal credential을 함께 발급하는 로그인 계약이다.
 *
 * 발급된 credential의 실제 형식과 저장 방식은 인프라 계층이 결정하며, 호출자는 결과와 만료 시각만 사용한다.
 */
interface GrantAuthenticationUseCase {

    /** 사용자 자격증명을 검증하고 새로운 인증정보를 발급한다. */
    suspend fun grant(command: Command): Result

    /** 로그인에 필요한 사용자 입력값이다. raw password는 영속화되지 않는다. */
    data class Command(

        val principal: String,

        val rawPassword: String

    )

    /** 로그인 결과와 발급된 두 credential을 표현한다. */
    data class Result(

        val userId: String,

        val accessCredential: Credential,

        val renewalCredential: Credential

    )

    /** 외부에 전달할 credential 값과 만료 시각이다. */
    data class Credential(

        val serializedValue: String,

        val expiresAt: Instant

    )

}
