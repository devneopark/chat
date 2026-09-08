package com.devneopark.chat.restapi.context.iam.application.port.inbound

import kotlin.time.Instant

/**
 * renewal credential을 한 번 사용해 새로운 access·renewal credential을 발급하는 계약이다.
 *
 * 재발급은 기존 AuthenticationGrant를 삭제하고 새 Grant를 등록하는 회전 방식으로 동작한다.
 */
interface RenewalAuthenticationUseCase {

    /** 유효한 renewal credential을 회전시키고 새로운 인증정보를 발급한다. */
    suspend fun renewal(command: Command): Result

    /** 재발급에 사용할 renewal credential 원문이다. */
    data class Command(

        val renewalCredentialId: String

    )

    /** 재발급 결과와 새 credential의 만료 시각을 표현한다. */
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
