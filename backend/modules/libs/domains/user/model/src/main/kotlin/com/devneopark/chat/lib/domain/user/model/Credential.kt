package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 사용자 애그리거트의 인증 정보.
 *
 * 사용자의 principal 값과 비밀번호 해시를 보관하며 인증 상태가 깨지지 않도록 최소 불변식을 보호한다.
 *
 * @param principal 사용자를 인증 흐름에서 구분하기 위한 principal 값.
 * @param passwordHash 저장된 비밀번호 해시의 초기 값.
 * @throws DomainRuleViolationException principal 또는 비밀번호 해시가 사용자 도메인 규칙을 만족하지 않는 경우.
 */
class Credential(

    /**
     * 사용자를 인증 흐름에서 구분하기 위한 principal 값.
     */
    val principal: String,

    passwordHash: String

) {

    /**
     * 저장된 비밀번호 해시.
     */
    var passwordHash: String = passwordHash
        private set

    init {
        checkPrincipal(principal)
        checkPasswordHash(passwordHash)
    }

    internal fun changePasswordHash(newPasswordHash: String) {
        checkPasswordHash(newPasswordHash)
        this.passwordHash = newPasswordHash
    }

    private fun checkPrincipal(principal: String) {
        if (principal.isBlank() || principal.length > 36) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_PRINCIPAL
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    private fun checkPasswordHash(passwordHash: String) {
        if (passwordHash.isBlank()) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_PASSWORD
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
