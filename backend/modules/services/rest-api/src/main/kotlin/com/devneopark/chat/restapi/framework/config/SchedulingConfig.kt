package com.devneopark.chat.restapi.framework.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** 애플리케이션의 주기적 유지보수 작업 실행을 활성화한다. */
@Configuration
@EnableScheduling
class SchedulingConfig
