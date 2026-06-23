package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 사용자 애그리거트의 프로필.
 *
 * 사용자에게 노출되는 표시 이름을 보관하며 프로필 상태가 깨지지 않도록 최소 불변식을 보호한다.
 *
 * @param displayName 표시 이름의 초기 값.
 * @throws DomainRuleViolationException 표시 이름이 사용자 도메인 규칙을 만족하지 않는 경우.
 */
class Profile(

    displayName: String

) {

    /**
     * 사용자 표시 이름.
     */
    var displayName: String = displayName
        private set

    init {
        checkDisplayName(displayName)
    }

    internal fun changeDisplayName(newDisplayName: String) {
        checkDisplayName(newDisplayName)
        this.displayName = newDisplayName
    }

    private fun checkDisplayName(displayName: String) {
        if (displayName.isBlank() || displayName.length > 20) {
            val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
