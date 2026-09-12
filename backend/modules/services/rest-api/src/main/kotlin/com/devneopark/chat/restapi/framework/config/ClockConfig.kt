package com.devneopark.chat.restapi.framework.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }

    @Bean
    fun temporalAdjuster(): TemporalAdjuster {
        return TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    }

    @Bean
    fun defaultTimezone(): ZoneOffset {
        return ZoneOffset.UTC
    }

}