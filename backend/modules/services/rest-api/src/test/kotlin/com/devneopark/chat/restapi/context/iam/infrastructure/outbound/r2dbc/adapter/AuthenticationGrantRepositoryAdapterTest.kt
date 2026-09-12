package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.authentication_grant.model.AccessCredential
import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.authentication_grant.model.RenewalCredential
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.maintenance.AuthenticationGrantPartitionManager
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository.AuthenticationGrantEntityRepository
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Instant

@DataR2dbcTest
@ActiveProfiles("test")
@Import(AuthenticationGrantRepositoryAdapter::class)
class AuthenticationGrantRepositoryAdapterTest {

    @Autowired
    lateinit var authenticationGrantRepositoryAdapter: AuthenticationGrantRepositoryAdapter

    @Autowired
    lateinit var authenticationGrantEntityRepository: AuthenticationGrantEntityRepository

    @Autowired
    lateinit var databaseClient: DatabaseClient

    companion object {

        private val schemaName = "rest_api_" + UUID.randomUUID()
            .toString()
            .replace("-", "")

        @JvmStatic
        @DynamicPropertySource
        fun setProperties(registry: DynamicPropertyRegistry) {
            val db = SharedPostgresContainer.container
            DriverManager.getConnection(
                db.jdbcUrl,
                db.username,
                db.password
            ).use {
                it.createStatement().use {
                    it.execute("create schema if not exists \"${schemaName}\"")
                }
            }
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${db.host}:${db.firstMappedPort}/${db.databaseName}?schema=${schemaName}"
            }
            registry.add("spring.r2dbc.username", db::getUsername)
            registry.add("spring.r2dbc.password", db::getPassword)
            registry.add("spring.sql.init.schema-locations") {
                "classpath:init/AuthenticationGrantRepositoryAdapterTest-schema.sql"
            }
            registry.add("spring.sql.init.data-locations") {
                "classpath:init/AuthenticationGrantRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `insert하면 grant를 저장하고 access jti와 renewal credential id로 조회할 수 있다`() = runTest {
        // given
        val grant = AuthenticationGrant(
            AuthenticationGrant.Id("grant-001"),
            User.Id("user-001"),
            Instant.parse("2026-08-11T00:00:00Z"),
            AccessCredential(
                AccessCredential.Id("access-jti-001"),
                Instant.parse("2026-08-11T00:00:01Z"),
                Instant.parse("2026-08-11T00:15:01Z")
            ),
            RenewalCredential(
                RenewalCredential.Id("renewal-id-001"),
                Instant.parse("2026-08-11T00:00:02Z"),
                Instant.parse("2026-08-18T00:00:02Z")
            )
        )

        // when
        val result = authenticationGrantRepositoryAdapter.insert(grant)
        val byJti = authenticationGrantRepositoryAdapter.findByJti("access-jti-001")
        val byRenewalCredentialId = authenticationGrantRepositoryAdapter
            .findByRenewalCredentialIdForUpdate("renewal-id-001")

        // then
        assertSame(grant, result)
        assertNotNull(byJti)
        assertEquals("grant-001", byJti.id.value)
        assertEquals("user-001", byJti.userId.value)
        assertEquals("access-jti-001", byJti.accessCredential.id.value)
        assertEquals("renewal-id-001", byJti.renewalCredential.id.value)
        assertNotNull(byRenewalCredentialId)
        assertEquals("grant-001", byRenewalCredentialId.id.value)
    }

    @Test
    fun `access jti로 grant를 삭제하면 access와 renewal 조회 결과가 모두 사라진다`() = runTest {
        // when
        authenticationGrantRepositoryAdapter.deleteByJti("seed-access-jti-001")

        // then
        assertNull(authenticationGrantRepositoryAdapter.findByJti("seed-access-jti-001"))
        assertNull(authenticationGrantRepositoryAdapter.findByRenewalCredentialIdForUpdate("seed-renewal-id-001"))
    }

    @Test
    fun `renewal credential id로 grant를 삭제하면 access와 renewal 조회 결과가 모두 사라진다`() = runTest {
        // given
        val grant = AuthenticationGrant(
            AuthenticationGrant.Id("grant-002"),
            User.Id("user-002"),
            Instant.parse("2026-08-11T00:00:00Z"),
            AccessCredential(
                AccessCredential.Id("access-jti-002"),
                Instant.parse("2026-08-11T00:00:01Z"),
                Instant.parse("2026-08-11T00:15:01Z")
            ),
            RenewalCredential(
                RenewalCredential.Id("renewal-id-002"),
                Instant.parse("2026-08-11T00:00:02Z"),
                Instant.parse("2026-08-18T00:00:02Z")
            )
        )
        authenticationGrantRepositoryAdapter.insert(grant)

        // when
        authenticationGrantRepositoryAdapter.deleteByRenewalCredentialId("renewal-id-002")

        // then
        assertNull(authenticationGrantRepositoryAdapter.findByJti("access-jti-002"))
        assertNull(authenticationGrantRepositoryAdapter.findByRenewalCredentialIdForUpdate("renewal-id-002"))
    }

    @Test
    fun `주어진 시각이 속한 주간 파티션을 생성한다`() = runTest {
        // given
        val manager = AuthenticationGrantPartitionManager(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
            ZoneOffset.UTC,
            databaseClient,
            20 * 24 * 60 * 60 * 1_000L
        )

        // when
        manager.ensurePartition(java.time.Instant.parse("2026-10-12T00:00:00Z"))

        // then
        assertNotNull(partitionName("20261012"))
    }

    @Test
    fun `주간 파티션을 생성하고 지난주 파티션을 삭제하며 2주 전 파티션 삭제를 재시도한다`() = runTest {
        // given
        val manager = AuthenticationGrantPartitionManager(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
            ZoneOffset.UTC,
            databaseClient,
            20 * 24 * 60 * 60 * 1_000L
        )

        // when
        val now = java.time.Instant.parse("2026-09-09T00:00:00Z")
        manager.ensureUpcomingPartitions(now)
        manager.dropPartitionFromTwoWeeksAgo(now)
        manager.dropPreviousWeekPartition(now)

        // then
        assertNull(partitionName("20260824"))
        assertNull(partitionName("20260831"))
        assertNotNull(partitionName("20260907"))
        assertNotNull(partitionName("20260914"))
        assertNotNull(partitionName("20260928"))
        assertNotNull(partitionName("20261005"))
    }

    private suspend fun partitionName(partitionDate: String): String? {
        return databaseClient.sql(
            "select to_regclass('authentication_grant_$partitionDate') as partition_name"
        )
            .map { row -> row.get("partition_name", String::class.java) ?: "" }
            .one()
            .awaitSingle()
            .ifBlank { null }
    }

}
