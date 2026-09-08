package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.DefaultApplicationArguments

@ExtendWith(MockitoExtension::class)
class AuthenticationGrantPartitionSchedulerTest {

    @Mock
    lateinit var authenticationGrantPartitionManager: AuthenticationGrantPartitionManager

    @Test
    fun `애플리케이션 시작 시 필요한 파티션을 생성한다`() = runTest {
        // given
        val scheduler = AuthenticationGrantPartitionScheduler(authenticationGrantPartitionManager)

        // when
        scheduler.run(DefaultApplicationArguments())

        // then
        verify(authenticationGrantPartitionManager).ensureUpcomingPartitions()
    }

    @Test
    fun `수요일 유지보수 시 미래 파티션을 보장한 뒤 지난주 파티션을 삭제한다`() = runTest {
        // given
        val scheduler = AuthenticationGrantPartitionScheduler(authenticationGrantPartitionManager)

        // when
        scheduler.maintainPartitions()

        // then
        val inOrder = inOrder(authenticationGrantPartitionManager)
        inOrder.verify(authenticationGrantPartitionManager).ensureUpcomingPartitions()
        inOrder.verify(authenticationGrantPartitionManager).dropPreviousWeekPartition()
    }

}
