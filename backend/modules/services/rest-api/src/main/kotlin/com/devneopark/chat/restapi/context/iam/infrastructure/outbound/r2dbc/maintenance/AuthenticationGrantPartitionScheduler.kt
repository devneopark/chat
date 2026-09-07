package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AuthenticationGrantPartitionScheduler(

    private val authenticationGrantPartitionManager: AuthenticationGrantPartitionManager

) : ApplicationRunner {

    override fun run(args: ApplicationArguments) = runBlocking {
        authenticationGrantPartitionManager.ensureUpcomingPartitions()
    }

    @Scheduled(cron = "0 0 0 * * WED", zone = "UTC")
    fun maintainPartitions() = runBlocking {
        authenticationGrantPartitionManager.ensureUpcomingPartitions()
        authenticationGrantPartitionManager.dropPreviousWeekPartition()
    }

}
