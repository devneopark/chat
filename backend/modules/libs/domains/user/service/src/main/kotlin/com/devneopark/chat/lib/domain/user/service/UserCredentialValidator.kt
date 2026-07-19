package com.devneopark.chat.lib.domain.user.service

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 사용자 인증 정보 정책 검증기.
 *
 * 외부에서 주입된 정규식을 사용해 principal 값과 원문 비밀번호가 현재 사용자 정책을 만족하는지 확인한다.
 * 정책을 만족하지 않는 값에는 사용자 도메인 규칙 위반 예외를 던진다.
 *
 * @param principalRegex principal 값에 적용할 허용 형식 정규식.
 * @param passwordRegex 원문 비밀번호에 적용할 허용 형식 정규식.
 */
class UserCredentialValidator(

    private val principalRegex: Regex,

    private val passwordRegex: Regex

) {

    /**
     * principal 값이 사용자 인증 정책을 만족하는지 확인한다.
     *
     * @param principal 검증할 principal 값.
     * @throws DomainRuleViolationException principal 값이 [principalRegex]와 일치하지 않는 경우.
     */
    fun validatePrincipal(principal: CharSequence) {
        if (!principalRegex.matches(principal)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_PRINCIPAL
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /**
     * 원문 비밀번호가 사용자 인증 정책을 만족하는지 확인한다.
     *
     * @param password 검증할 원문 비밀번호.
     * @throws DomainRuleViolationException 비밀번호가 [passwordRegex]와 일치하지 않는 경우.
     */
    fun validatePassword(password: CharSequence) {
        if (!passwordRegex.matches(password)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_PASSWORD
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
