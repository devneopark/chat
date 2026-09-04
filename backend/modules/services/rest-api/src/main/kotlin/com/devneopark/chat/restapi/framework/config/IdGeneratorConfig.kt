package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.libs.shared.application.identifier.HyphenlessUuidGenerator
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdGeneratorConfig {

    @Bean
    fun idGenerator(): IdGenerator {
        return HyphenlessUuidGenerator()
    }

}