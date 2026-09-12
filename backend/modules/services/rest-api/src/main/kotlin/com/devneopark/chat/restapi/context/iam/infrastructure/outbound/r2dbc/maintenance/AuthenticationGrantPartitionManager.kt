package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * `AuthenticationGrant`를 `rc_will_expires_at` 기준의 UTC 주간 파티션으로 관리한다.
 *
 * 각 파티션은 UTC 월요일 00:00부터 다음 월요일 00:00까지의 만료 시각을 담당한다.
 * 애플리케이션 시작 시 현재 주간 파티션을 보장하고, 정기 유지보수 시에는
 * 다음 유지보수 시점까지 발급될 renewal credential의 만료 시각을 수용할 수 있도록
 * `refreshTokenTtlMillis`에 1주를 더한 범위까지 미래 파티션을 보장한다.
 * 지난주 파티션은 해당 파티션의 모든 renewal credential이 만료된 뒤 삭제한다.
 * 2주 전 파티션도 함께 삭제해 직전 유지보수에서 실패한 삭제를 한 번 재시도한다.
 * 파티션 DDL은 `IF NOT EXISTS`와 `IF EXISTS`를 사용해 반복 실행할 수 있도록 구성한다.
 */
@Component
class AuthenticationGrantPartitionManager(

    private val databaseClient: DatabaseClient,

    private val clock: Clock,

    private val transactionalOperator: TransactionalOperator,

    @Value($$"${chat.infrastructure.auth.refreshToken.ttlMillis}")
    private val refreshTokenTtlMillis: Long

) {

    /**
     * 주어진 시각이 속한 UTC 주간 파티션을 생성한다.
     *
     * 애플리케이션이 요청을 받기 전에 현재 주간 파티션을 준비할 때 호출한다.
     * 이미 파티션이 존재하면 아무 작업도 하지 않으므로 애플리케이션 재기동 시에도
     * 안전하게 반복 호출할 수 있다.
     *
     * @param now 현재 시각
     */
    suspend fun ensurePartition(now: Instant) {
        val partitionWeekStart = now
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextWeekStart = partitionWeekStart.plusWeeks(1)
        val format = DateTimeFormatter.BASIC_ISO_DATE
        val formattedDate = partitionWeekStart.format(format)
        val partitionName = "authentication_grant_${formattedDate}"

        transactionalOperator.executeAndAwait {
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
        }
    }

    /**
     * 다음 유지보수 시점까지 발급할 수 있는 기간을 포함하도록 미래 파티션을 생성한다.
     *
     * 유지보수 주기가 1주이므로 현재 시각의 다음 주부터
     * `현재 시각 + refreshTokenTtlMillis`가 속한 주의 다음 주까지 생성한다.
     * 이 추가 1주는 다음 유지보수 실행 전까지 발급되는 credential의 만료 시각을
     * 수용하기 위한 버퍼다. 이미 존재하는 파티션은 건너뛰므로 반복 실행할 수 있다.
     */
    suspend fun ensureUpcomingPartitions() {
        transactionalOperator.executeAndAwait {
            val now = clock.instant()
            val currentWeekStart = now
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val lastRequiredWeekStart = now
                .plusMillis(refreshTokenTtlMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .let { ttlWeekStart ->
                    maxOf(currentWeekStart.plusWeeks(1), ttlWeekStart.plusWeeks(1))
                }
            var partitionWeekStart = currentWeekStart.plusWeeks(1)

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

    /**
     * 현재 주의 직전 주간 파티션을 삭제한다.
     *
     * 파티션 키가 renewal credential의 만료 시각이므로 현재 주가 시작되면
     * 직전 주 파티션의 credential은 모두 만료된 상태다. 스케줄러는 매주 수요일에
     * 이 메서드를 호출하며, 파티션이 이미 삭제된 경우에도 안전하게 종료한다.
     */
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

    /**
     * 현재 주의 2주 전 파티션을 삭제한다.
     *
     * 직전 유지보수에서 직전 주 파티션 삭제가 실패했을 가능성을 고려한 재시도다.
     * 스케줄러는 직전 주 파티션 삭제와 함께 이 메서드도 호출해 파티션별 삭제를
     * 최대 한 번 더 시도한다. 파티션이 이미 삭제되었거나 존재하지 않아도 안전하게 종료한다.
     */
    suspend fun dropPartitionFromTwoWeeksAgo() {
        transactionalOperator.executeAndAwait<Unit> {
            val twoWeeksAgoStart = clock.instant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(2)
            val format = DateTimeFormatter.BASIC_ISO_DATE
            val formattedDate = twoWeeksAgoStart.format(format)
            val partitionName = "authentication_grant_${formattedDate}"
            databaseClient.sql("drop table if exists $partitionName")
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }

}
