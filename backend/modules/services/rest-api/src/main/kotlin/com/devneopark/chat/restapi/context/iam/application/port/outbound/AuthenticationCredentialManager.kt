package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.time.Instant

/**
 * access JWT와 renewal credential을 발급하고 이를 AuthenticationGrant로 묶는 인프라 계약이다.
 *
 * renewal credential은 원문 값이 외부로 전달되며, AuthenticationGrant에는 재발급과 폐기에 필요한 값이 저장된다.
 */
interface AuthenticationCredentialManager {

    /** 사용자와 Grant 식별자를 기준으로 access·renewal credential 세트를 발급한다. */
    suspend fun issue(grantId: AuthenticationGrant.Id, userId: UserId, now: Instant): CredentialSet

    /** 발급된 credential 값, 만료 시각, 영속화할 AuthenticationGrant를 함께 반환한다. */
    data class CredentialSet(

        val authenticationGrant: AuthenticationGrant,

        val serializedCredentialValue: String,

        val serializedRenewalCredentialValue: String,

        val accessCredentialExpiresAt: Instant,

        val renewalCredentialExpiresAt: Instant

    )

}
