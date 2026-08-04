package com.devneopark.chat.lib.domain.authentication_grant.model

import com.devneopark.chat.lib.domain.authentication_grant.reference.AuthenticationGrantId
import com.devneopark.chat.lib.domain.authentication_grant.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Instant

/**
 * 하나의 로그인 세션이자 Access JWT 화이트리스트 레코드인 애그리거트 루트.
 *
 * JWT 원문과 Renewal Credential 원문·해시는 이 객체에 저장하지 않는다.
 *
 * Access JWT의 `jti`로 이 Grant를 조회하고 현재 Access Credential과 일치하는지 확인해 화이트리스트를 적용한다.
 * Grant 전체를 폐기할 때는 저장소에서 이 레코드를 삭제한다.
 *
 * 감사와 이력 보존은 이 애그리거트와 현재 상태 영속 모델의 책임이 아니며, 발급·rotation·폐기·credential 불일치 거절 등의 기록은
 * 접속 로그 또는 보안 로그가 append-only 방식으로 보존한다.
 */
class AuthenticationGrant(

    val id: Id,

    val userId: UserId,

    val issuedAt: Instant,

    accessCredential: AccessCredential,

    renewalCredential: RenewalCredential

) {

    /**
     * 현재 Access Credential.
     */
    var accessCredential: AccessCredential = accessCredential
        private set

    /**
     * 현재 Renewal Credential.
     */
    var renewalCredential: RenewalCredential = renewalCredential
        private set

    init {
        checkCredentialIssuedAt(accessCredential, issuedAt)
        checkCredentialIssuedAt(renewalCredential, issuedAt)
    }

    /**
     * 현재 Renewal Credential을 검증하고 Access/Renewal Credential을 새 쌍으로 교체한다.
     *
     * 이전 credential set은 현재 애그리거트 상태에서 제거된다. 저장소에서는 현재 Renewal
     * Credential ID 확인과 두 credential 교체를 하나의 원자적 조건부 갱신으로 수행해야 한다.
     * 이전 Renewal Credential이 다시 제출되면 해당 요청만 거절한다.
     */
    fun rotateCredentials(
        presentedRenewalCredentialId: RenewalCredential.Id,
        nextAccessCredential: AccessCredential,
        nextRenewalCredential: RenewalCredential,
        now: Instant
    ) {
        if (renewalCredential.id.value != presentedRenewalCredentialId.value) {
            val exceptionDefinition = ExceptionDefinition.RENEWAL_CREDENTIAL_MISMATCH
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }

        val isRenewalCredentialUsable = renewalCredential.isUsableAt(now)
        if (!isRenewalCredentialUsable) {
            val exceptionDefinition = ExceptionDefinition.RENEWAL_CREDENTIAL_NOT_USABLE
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }

        checkCredentialIssuedAt(nextAccessCredential, now)
        checkCredentialIssuedAt(nextRenewalCredential, now)

        accessCredential = nextAccessCredential
        renewalCredential = nextRenewalCredential
    }

    private fun checkCredentialIssuedAt(credential: AccessCredential, lowerBound: Instant) {
        if (credential.issuedAt < lowerBound) {
            val exceptionDefinition = ExceptionDefinition.CREDENTIAL_ISSUED_AT_INVALID
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    private fun checkCredentialIssuedAt(credential: RenewalCredential, lowerBound: Instant) {
        if (credential.issuedAt < lowerBound) {
            val exceptionDefinition = ExceptionDefinition.CREDENTIAL_ISSUED_AT_INVALID
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /**
     * AuthenticationGrant 애그리거트에서 사용하는 식별자.
     */
    class Id(

        override val value: String

    ) : AuthenticationGrantId {

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
                val exceptionDefinition = ExceptionDefinition.AUTHENTICATION_GRANT_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
