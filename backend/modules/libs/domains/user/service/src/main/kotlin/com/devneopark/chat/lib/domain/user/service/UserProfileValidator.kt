package com.devneopark.chat.lib.domain.user.service

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 사용자 프로필 정책 검증기.
 *
 * 외부에서 주입된 정규식을 사용해 표시 이름이 현재 사용자 프로필 정책을 만족하는지 확인한다.
 * 정책을 만족하지 않는 값에는 사용자 도메인 규칙 위반 예외를 던진다.
 *
 * @param displayNameRegex 표시 이름에 적용할 허용 형식 정규식.
 */
class UserProfileValidator(

    private val displayNameRegex: Regex

) {

    /**
     * 표시 이름이 사용자 프로필 정책을 만족하는지 확인한다.
     *
     * @param displayName 검증할 표시 이름.
     * @throws DomainRuleViolationException 표시 이름이 [displayNameRegex]와 일치하지 않는 경우.
     */
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
