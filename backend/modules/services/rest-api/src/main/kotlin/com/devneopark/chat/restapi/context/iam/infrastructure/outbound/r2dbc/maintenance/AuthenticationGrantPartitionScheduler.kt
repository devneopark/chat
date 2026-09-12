package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** 매주 수요일 인증정보 파티션 생성과 삭제 유지보수 작업을 실행한다. */
@Component
class AuthenticationGrantPartitionScheduler(

    private val authenticationGrantPartitionManager: AuthenticationGrantPartitionManager

) {

    /** 미래 파티션을 보장한 뒤 지난주 파티션을 삭제하고 2주 전 파티션 삭제를 재시도한다. */
    @Scheduled(cron = "0 0 0 * * WED", zone = "UTC")
    fun maintainPartitions() = runBlocking {
        authenticationGrantPartitionManager.ensureUpcomingPartitions()
        authenticationGrantPartitionManager.dropPartitionFromTwoWeeksAgo()
        authenticationGrantPartitionManager.dropPreviousWeekPartition()
    }

}
