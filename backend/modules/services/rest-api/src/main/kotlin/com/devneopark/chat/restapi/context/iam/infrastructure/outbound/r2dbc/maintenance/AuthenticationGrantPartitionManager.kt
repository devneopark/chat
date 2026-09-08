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

/**
 * `rc_will_expires_at` 기준 주간 파티션의 생성과 삭제를 담당한다.
 *
 * 파티션은 UTC 월요일을 시작점으로 하며, 미래 파티션은 renewal credential TTL까지 보장한다.
 */
@Component
class AuthenticationGrantPartitionManager(

    private val databaseClient: DatabaseClient,

    private val clock: Clock,

    private val transactionalOperator: TransactionalOperator,

    @Value($$"${chat.infrastructure.auth.refreshToken.ttlMillis}")
    private val refreshTokenTtlMillis: Long

) {

    /** 애플리케이션이 발급할 수 있는 기간을 포함하도록 미래 파티션을 생성한다. */
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

    /** 현재 주의 직전 주간 파티션을 삭제한다. 스케줄러는 매주 수요일에 이 작업을 호출한다. */
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
