package com.devneopark.chat.lib.domain.authentication_grant.model

import com.devneopark.chat.lib.domain.authentication_grant.reference.AccessCredentialId
import com.devneopark.chat.lib.domain.authentication_grant.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Instant

/**
 * Access JWT의 jti와 생명주기만 표현하는 도메인 객체.
 *
 * JWT 원문은 이 객체에 포함하지 않는다.
 */
class AccessCredential(

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
            val exceptionDefinition = ExceptionDefinition.INVALID_ACCESS_CREDENTIAL_EXPIRATION
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    class Id(

        override val value: String

    ) : AccessCredentialId {

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
                val exceptionDefinition = ExceptionDefinition.ACCESS_CREDENTIAL_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
