package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.restapi.context.room.application.policy.AdmissionSlotPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RoomContextPolicyConfig {

    @Bean
    fun admissionSlotPolicy(
        @Value($$"${chat.application.room.policy.validation.capacity}")
        maxCapacity: Int
    ): AdmissionSlotPolicy {
        return AdmissionSlotPolicy(maxCapacity)
    }

}