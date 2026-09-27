package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.lib.domain.room.service.RoomInfoValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class RoomDomainServiceConfig(

    private val roomInfoPattern: RoomInfoPattern

) {

    @Bean
    fun roomInfoValidator(): RoomInfoValidator {
        val titleRegex = Regex(roomInfoPattern.title)
        val passwordRegex = Regex(roomInfoPattern.password)
        return RoomInfoValidator(titleRegex, passwordRegex)
    }

    @Component
    data class RoomInfoPattern(

        @Value($$"${chat.domain.room.service.validation.info.pattern.title}")
        val title: String,

        @Value($$"${chat.domain.room.service.validation.info.pattern.password}")
        val password: String

    )

}