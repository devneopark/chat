package com.devneopark.chat.restapi.context.iam.application.port.outbound

/**
 * 직렬화된 access credential의 위조 여부와 기본 claims를 검증하는 인프라 계약이다.
 *
 * 이 계약은 세부 인가를 수행하지 않으며, 검증 결과의 사용자 식별자와 JTI만 반환한다.
 */
interface AccessCredentialVerifier {

    /** access credential을 검증하고 사용자 식별자와 JTI를 반환한다. */
    suspend fun verify(serializedCredential: String): VerifiedCredential

    /** 서명 검증을 통과한 access credential의 식별 정보다. */
    data class VerifiedCredential(

        val userId: String,

        val jti: String

    )

}
