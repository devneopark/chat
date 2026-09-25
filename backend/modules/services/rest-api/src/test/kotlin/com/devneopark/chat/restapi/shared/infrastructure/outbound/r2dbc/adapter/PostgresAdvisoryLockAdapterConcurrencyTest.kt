package com.devneopark.chat.restapi.shared.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertNull

@DataR2dbcTest
@ActiveProfiles("test")
@Import(PostgresAdvisoryLockAdapter::class)
class PostgresAdvisoryLockAdapterConcurrencyTest {

    @Autowired
    lateinit var postgresAdvisoryLockAdapter: PostgresAdvisoryLockAdapter

    @Autowired
    lateinit var connectionFactory: ConnectionFactory

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
        }

    }

    @Test
    fun `advisory lock은 트랜잭션이 종료될 때까지 유지된다`() = runTest {
        // given
        val transactionOperator = TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
        val lockKey = "room:capacity:room-001"
        val firstLockAcquired = CompletableDeferred<Unit>()
        val releaseFirstTransaction = CompletableDeferred<Unit>()
        val secondLockAcquired = CompletableDeferred<Unit>()

        val firstTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                postgresAdvisoryLockAdapter.lock(lockKey)
                firstLockAcquired.complete(Unit)
                releaseFirstTransaction.await()
            }
        }
        firstLockAcquired.await()

        val secondTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                postgresAdvisoryLockAdapter.lock(lockKey)
                secondLockAcquired.complete(Unit)
            }
        }

        // when & then
        try {
            assertNull(withTimeoutOrNull(300) { secondLockAcquired.await() })
        } finally {
            releaseFirstTransaction.complete(Unit)
        }

        firstTransaction.await()
        withTimeout(1_000) { secondLockAcquired.await() }
        secondTransaction.await()
    }

}
