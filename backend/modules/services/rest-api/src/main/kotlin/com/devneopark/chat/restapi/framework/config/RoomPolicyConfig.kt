package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.restapi.context.room.application.policy.AdmissionSlotPolicy
import com.devneopark.chat.restapi.context.room.application.policy.RoomInfoPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class RoomPolicyConfig(

    private val roomInfoPattern: RoomInfoPattern

) {

    @Bean
    fun admissionSlotPolicy(
        @Value($$"${chat.application.room.policy.validation.capacity}")
        maxCapacity: Int
    ): AdmissionSlotPolicy {
        return AdmissionSlotPolicy(maxCapacity)
    }

    @Bean
    fun roomInfoPolicy(): RoomInfoPolicy {
        val titleRegex = Regex(roomInfoPattern.title)
        val passwordRegex = Regex(roomInfoPattern.password)
        return RoomInfoPolicy(titleRegex, passwordRegex)
    }

    @Component
    data class RoomInfoPattern(

        @Value($$"${chat.application.room.policy.validation.info.pattern.title}")
        val title: String,

        @Value($$"${chat.application.room.policy.validation.info.pattern.password}")
        val password: String

    )

}
