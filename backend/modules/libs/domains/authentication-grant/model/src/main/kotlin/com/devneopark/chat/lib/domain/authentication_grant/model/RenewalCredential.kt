package com.devneopark.chat.lib.domain.authentication_grant.model

import com.devneopark.chat.lib.domain.authentication_grant.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.authentication_grant.reference.RenewalCredentialId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Instant

/**
 * 현재 로그인 세션에서 사용할 Renewal Credential.
 *
 * Rotation 시 이전 credential은 애그리거트와 영속 모델에서 제거되므로 소비 시각을
 * 현재 상태로 보관하지 않는다. 제출된 credential의 유효성은 현재 credential ID와의 일치 여부로 판단한다.
 */
class RenewalCredential(

    val id: Id,

    val issuedAt: Instant,

    val willExpiresAt: Instant

) {

    init {
        checkExpirationTimestamp(willExpiresAt, issuedAt)
    }

    internal fun isUsableAt(now: Instant): Boolean {
        return now in issuedAt ..< willExpiresAt
    }

    private fun checkExpirationTimestamp(willExpiresAt: Instant, issuedAt: Instant) {
        if (willExpiresAt <= issuedAt) {
            val exceptionDefinition = ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL_EXPIRATION
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    class Id(

        override val value: String

    ) : RenewalCredentialId {

        init {
            checkValue(value)
        }

        companion object {

            fun from(value: String): Id {
                return Id(value)
            }

        }

        internal fun checkValue(value: String) {
            if (value.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.RENEWAL_CREDENTIAL_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
