package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@DataR2dbcTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ParticipantRepositoryAdapter::class)
class ParticipantRepositoryAdapterConcurrencyTest {

    @Autowired
    lateinit var participantRepositoryAdapter: ParticipantRepositoryAdapter

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
                "classpath:init/ParticipantRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `활성 참여자가 행 잠금 중이면 for update 조회는 잠금 해제까지 대기한다`() = runTest {
        // given
        val roomId = "participant-room-001"
        val userId = "participant-user-004"
        val participantId = "participant-active-guest-001"
        val transactionOperator = TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
        val rowLockAcquired = CompletableDeferred<Unit>()
        val releaseRowLock = CompletableDeferred<Unit>()
        val updateQueryStarted = CompletableDeferred<Unit>()

        val lockingTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                databaseClient.sql(
                    """
                    select id
                    from participant
                    where id = :id
                    for update
                    """.trimIndent()
                )
                    .bind("id", participantId)
                    .fetch()
                    .one()
                    .awaitSingle()

                rowLockAcquired.complete(Unit)
                releaseRowLock.await()
            }
        }
        rowLockAcquired.await()

        val updateTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                updateQueryStarted.complete(Unit)
                participantRepositoryAdapter.findActiveByRoomIdAndUserIdForUpdate(
                    Room.Id(roomId),
                    User.Id(userId)
                )
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
            releaseRowLock.complete(Unit)
            val result = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(2_000) { updateTransaction.await() }
            }
            assertNotNull(result)
            assertEquals(participantId, result.id.value)
        } finally {
            releaseRowLock.complete(Unit)
            lockingTransaction.await()
        }
    }

    @Test
    fun `가장 오래된 게스트가 행 잠금 중이면 후보 조회는 잠금 해제까지 대기한다`() = runTest {
        // given
        val roomId = "participant-room-001"
        val participantId = "participant-active-guest-001"
        val transactionOperator = TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
        val rowLockAcquired = CompletableDeferred<Unit>()
        val releaseRowLock = CompletableDeferred<Unit>()
        val updateQueryStarted = CompletableDeferred<Unit>()

        val lockingTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                databaseClient.sql(
                    """
                    select id
                    from participant
                    where id = :id
                    for update
                    """.trimIndent()
                )
                    .bind("id", participantId)
                    .fetch()
                    .one()
                    .awaitSingle()

                rowLockAcquired.complete(Unit)
                releaseRowLock.await()
            }
        }
        rowLockAcquired.await()

        val updateTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                updateQueryStarted.complete(Unit)
                participantRepositoryAdapter.findOldestActiveGuestForUpdate(Room.Id(roomId))
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
            releaseRowLock.complete(Unit)
            val result = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(2_000) { updateTransaction.await() }
            }
            assertNotNull(result)
            assertEquals(participantId, result.id.value)
        } finally {
            releaseRowLock.complete(Unit)
            lockingTransaction.await()
        }
    }

}
