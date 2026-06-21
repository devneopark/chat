package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.kernel.exception.DomainRuleViolationException

class Credential(

    val principal: String,

    passwordHash: String

) {

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