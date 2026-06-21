package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.kernel.exception.DomainRuleViolationException

class Profile(

    displayName: String

) {

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