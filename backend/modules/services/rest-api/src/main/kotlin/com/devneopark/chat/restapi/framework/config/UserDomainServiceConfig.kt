package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.lib.domain.user.service.UserCredentialValidator
import com.devneopark.chat.lib.domain.user.service.UserProfileValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class UserDomainServiceConfig(

    private val userCredentialPattern: UserCredentialPattern,

    private val userProfilePattern: UserProfilePattern

) {

    @Bean
    fun userCredentialValidator(): UserCredentialValidator {
        val principalRegex = Regex(userCredentialPattern.principal)
        val passwordRegex = Regex(userCredentialPattern.password)
        return UserCredentialValidator(principalRegex, passwordRegex)
    }

    @Bean
    fun userProfileValidator(): UserProfileValidator {
        val displayNameRegex = Regex(userProfilePattern.displayName)
        return UserProfileValidator(displayNameRegex)
    }

    @Component
    data class UserCredentialPattern(

        @Value($$"${chat.domain.user.service.validation.credential.pattern.principal}")
        val principal: String,

        @Value($$"${chat.domain.user.service.validation.credential.pattern.password}")
        val password: String

    )

    @Component
    data class UserProfilePattern(

        @Value($$"${chat.domain.user.service.validation.profile.pattern.displayName}")
        val displayName: String

    )

}