package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class AuthenticationGrantPartitionInitializerTest {

    @Mock
    lateinit var authenticationGrantPartitionManager: AuthenticationGrantPartitionManager

    @Mock
    lateinit var clock: Clock

    @Test
    fun `singleton 초기화가 끝나면 현재와 미래 주간 파티션을 보장한다`() = runTest {
        // given
        val now = Instant.parse("2026-09-09T00:00:00Z")
        given(clock.instant()).willReturn(now)
        val initializer = AuthenticationGrantPartitionInitializer(
            clock,
            authenticationGrantPartitionManager
        )

        // when
        initializer.afterSingletonsInstantiated()

        // then
        val inOrder = inOrder(authenticationGrantPartitionManager)
        inOrder.verify(authenticationGrantPartitionManager).ensurePartition(now)
        inOrder.verify(authenticationGrantPartitionManager).ensureUpcomingPartitions(now)
    }

}
