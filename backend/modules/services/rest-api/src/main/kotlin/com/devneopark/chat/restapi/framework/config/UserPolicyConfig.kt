package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.restapi.context.user.application.policy.UserCredentialPolicy
import com.devneopark.chat.restapi.context.user.application.policy.UserProfilePolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class UserPolicyConfig(

    private val userCredentialPattern: UserCredentialPattern,

    private val userProfilePattern: UserProfilePattern

) {

    @Bean
    fun userCredentialPolicy(): UserCredentialPolicy {
        val principalRegex = Regex(userCredentialPattern.principal)
        val passwordRegex = Regex(userCredentialPattern.password)
        return UserCredentialPolicy(principalRegex, passwordRegex)
    }

    @Bean
    fun userProfilePolicy(): UserProfilePolicy {
        val displayNameRegex = Regex(userProfilePattern.displayName)
        return UserProfilePolicy(displayNameRegex)
    }

    @Component
    data class UserCredentialPattern(

        @Value($$"${chat.application.user.policy.validation.credential.pattern.principal}")
        val principal: String,

        @Value($$"${chat.application.user.policy.validation.credential.pattern.password}")
        val password: String

    )

    @Component
    data class UserProfilePattern(

        @Value($$"${chat.application.user.policy.validation.profile.pattern.displayName}")
        val displayName: String

    )

}
