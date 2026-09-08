package com.devneopark.chat.restapi.context.iam.application.port.inbound

/**
 * Access credential를 검증하고 요청 주체와 credential 식별자를 반환하는 인증 계약이다.
 *
 * 세부 인가는 이 계약의 책임이 아니며, 인증 성공 결과를 사용하는 응용 서비스가 수행한다.
 */
interface AuthenticateAccessCredentialUseCase {

    /** 서명과 화이트리스트를 검증해 access credential을 인증한다. */
    suspend fun authenticate(command: Command): Result

    /** 직렬화된 access credential을 인증 요청으로 전달한다. */
    data class Command(

        val serializedCredential: String

    )

    /** 인증된 사용자와 access credential의 JTI를 표현한다. */
    data class Result(

        val userId: String,

        val jti: String

    )

}
