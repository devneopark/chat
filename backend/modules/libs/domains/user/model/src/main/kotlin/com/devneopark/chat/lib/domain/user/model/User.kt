package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.kernel.exception.DomainRuleViolationException

class User(

    val id: UserId,

    val credential: Credential,

    val profile: Profile

) {

    fun changePassword(newPasswordHash: String) {
        credential.changePasswordHash(newPasswordHash)
    }

    fun changeDisplayName(newDisplayName: String) {
        profile.changeDisplayName(newDisplayName)
    }

    class Id(

        override val value: String

    ) : UserId {

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
                val exceptionDefinition = ExceptionDefinition.USER_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}