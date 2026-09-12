package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Clock

/** Netty가 시작되기 전에 현재 주간과 TTL 범위의 미래 인증정보 파티션을 준비하는 초기화기다. */
@Component
@Profile("local", "dev", "test")
@ConditionalOnProperty(
    prefix = "chat.infrastructure.auth.partition",
    name = ["initialize-on-startup"],
    havingValue = "true"
)
class AuthenticationGrantPartitionInitializer(

    private val clock: Clock,

    private val authenticationGrantPartitionManager: AuthenticationGrantPartitionManager

) : SmartInitializingSingleton {

    /** 모든 일반 singleton 초기화 이후, WebFlux 서버 시작 전에 현재와 미래 파티션을 보장한다. */
    override fun afterSingletonsInstantiated() = runBlocking {
        val now = clock.instant()
        authenticationGrantPartitionManager.ensurePartition(now)
        authenticationGrantPartitionManager.ensureUpcomingPartitions(now)
    }

}
