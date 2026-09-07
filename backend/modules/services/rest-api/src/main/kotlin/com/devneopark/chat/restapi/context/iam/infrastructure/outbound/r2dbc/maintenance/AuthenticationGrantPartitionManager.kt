package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Component
class AuthenticationGrantPartitionManager(

    private val databaseClient: DatabaseClient,

    private val clock: Clock,

    private val transactionalOperator: TransactionalOperator,

    @Value($$"${chat.infrastructure.auth.refreshToken.ttlMillis}")
    private val refreshTokenTtlMillis: Long

) {

    suspend fun ensureUpcomingPartitions() {
        transactionalOperator.executeAndAwait {
            val currentWeekStart = clock.instant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val lastRequiredWeekStart = clock.instant()
                .plusMillis(refreshTokenTtlMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            var partitionWeekStart = currentWeekStart

            while (!partitionWeekStart.isAfter(lastRequiredWeekStart)) {
                val nextWeekStart = partitionWeekStart.plusWeeks(1)
                val format = DateTimeFormatter.BASIC_ISO_DATE
                val formattedDate = partitionWeekStart.format(format)
                val partitionName = "authentication_grant_${formattedDate}"
                databaseClient.sql(
                    """
                        create table if not exists $partitionName
                            partition of authentication_grant
                            for values from ('${partitionWeekStart.atStartOfDay(ZoneOffset.UTC)}')
                                         to ('${nextWeekStart.atStartOfDay(ZoneOffset.UTC)}')
                        """.trimIndent()
                )
                    .fetch()
                    .rowsUpdated()
                    .awaitSingle()
                partitionWeekStart = nextWeekStart
            }
        }
    }

    suspend fun dropPreviousWeekPartition() {
        transactionalOperator.executeAndAwait<Unit> {
            val previousWeekStart = clock.instant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1)
            val format = DateTimeFormatter.BASIC_ISO_DATE
            val formattedDate = previousWeekStart.format(format)
            val partitionName = "authentication_grant_${formattedDate}"
            databaseClient.sql("drop table if exists $partitionName")
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }

}
