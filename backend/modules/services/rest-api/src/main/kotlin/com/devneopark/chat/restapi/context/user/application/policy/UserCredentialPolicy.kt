package com.devneopark.chat.restapi.context.user.application.policy

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/** 사용자 principal과 원문 비밀번호에 적용하는 입력 정책이다. */
class UserCredentialPolicy(

    private val principalRegex: Regex,

    private val passwordRegex: Regex

) {

    /** principal이 사용자 인증 정보 정책을 만족하는지 확인한다. */
    fun validatePrincipal(principal: CharSequence) {
        if (!principalRegex.matches(principal)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_PRINCIPAL
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /** 원문 비밀번호가 사용자 인증 정보 정책을 만족하는지 확인한다. */
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
