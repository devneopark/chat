package com.devneopark.chat.restapi.context.user.application.policy

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/** 사용자 표시 이름에 적용하는 프로필 입력 정책이다. */
class UserProfilePolicy(

    private val displayNameRegex: Regex

) {

    /** 표시 이름이 사용자 프로필 정책을 만족하는지 확인한다. */
    fun validateDisplayName(displayName: CharSequence) {
        if (!displayNameRegex.matches(displayName)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
