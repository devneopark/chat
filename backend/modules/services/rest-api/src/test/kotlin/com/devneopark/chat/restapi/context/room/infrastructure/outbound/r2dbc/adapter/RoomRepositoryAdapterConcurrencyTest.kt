package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@DataR2dbcTest
@ActiveProfiles("test")
@Import(RoomRepositoryAdapter::class)
class RoomRepositoryAdapterConcurrencyTest {

    @Autowired
    lateinit var roomRepositoryAdapter: RoomRepositoryAdapter

    @Autowired
    lateinit var databaseClient: DatabaseClient

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
            registry.add("spring.sql.init.data-locations") {
                "classpath:init/RoomRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `방이 공유 잠금 중이면 for update 조회는 잠금 해제까지 대기한다`() = runTest {
        // given
        val roomId = "room-close-001"
        val transactionOperator = TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
        val shareLockAcquired = CompletableDeferred<Unit>()
        val releaseShareLock = CompletableDeferred<Unit>()
        val updateQueryStarted = CompletableDeferred<Unit>()

        val sharingTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                databaseClient.sql(
                    """
                    select id
                    from room
                    where id = :id
                    for share
                    """.trimIndent()
                )
                    .bind("id", roomId)
                    .fetch()
                    .one()
                    .awaitSingle()

                shareLockAcquired.complete(Unit)
                releaseShareLock.await()
            }
        }
        shareLockAcquired.await()

        val updateTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                updateQueryStarted.complete(Unit)
                roomRepositoryAdapter.findActiveByIdForUpdate(Room.Id(roomId))
            }
        }
        updateQueryStarted.await()

        // when
        val completedWhileLocked = withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeoutOrNull(500) {
                updateTransaction.await()
                true
            } ?: false
        }

        // then
        try {
            assertFalse(completedWhileLocked)
            releaseShareLock.complete(Unit)
            assertNotNull(
                withContext(Dispatchers.Default.limitedParallelism(1)) {
                    withTimeout(2_000) { updateTransaction.await() }
                }
            )
        } finally {
            releaseShareLock.complete(Unit)
            sharingTransaction.await()
        }
    }

}
