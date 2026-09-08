package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** 애플리케이션 시작과 매주 수요일 유지보수 시 인증정보 파티션 관리 작업을 실행한다. */
@Component
class AuthenticationGrantPartitionScheduler(

    private val authenticationGrantPartitionManager: AuthenticationGrantPartitionManager

) : ApplicationRunner {

    /** 요청 처리 전에 현재 TTL 범위의 파티션을 준비한다. */
    override fun run(args: ApplicationArguments) = runBlocking {
        authenticationGrantPartitionManager.ensureUpcomingPartitions()
    }

    /** 미래 파티션을 보장한 뒤 지난주 파티션을 삭제한다. */
    @Scheduled(cron = "0 0 0 * * WED", zone = "UTC")
    fun maintainPartitions() = runBlocking {
        authenticationGrantPartitionManager.ensureUpcomingPartitions()
        authenticationGrantPartitionManager.dropPreviousWeekPartition()
    }

}
