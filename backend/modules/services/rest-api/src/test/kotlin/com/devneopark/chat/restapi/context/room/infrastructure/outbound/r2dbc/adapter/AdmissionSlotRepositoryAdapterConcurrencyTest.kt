package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
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
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals

@DataR2dbcTest
@ActiveProfiles("test")
@Import(AdmissionSlotRepositoryAdapter::class)
class AdmissionSlotRepositoryAdapterConcurrencyTest {

    @Autowired
    lateinit var admissionSlotRepositoryAdapter: AdmissionSlotRepositoryAdapter

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
                "classpath:init/AdmissionSlotRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `잠긴 빈 슬롯은 skip locked 조회 결과에서 제외된다`() = runTest {
        // given
        val roomId = "slot-empty-001"
        val lockedSlotNumber = 3
        val transactionOperator = TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
        val slotLocked = CompletableDeferred<Unit>()
        val releaseLockingTransaction = CompletableDeferred<Unit>()

        val lockingTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                databaseClient.sql(
                    """
                    select slot_number
                    from admission_slot
                    where room_id = :roomId
                        and slot_number = :slotNumber
                    for update
                    """.trimIndent()
                )
                    .bind("roomId", roomId)
                    .bind("slotNumber", lockedSlotNumber)
                    .fetch()
                    .one()
                    .awaitSingle()

                slotLocked.complete(Unit)
                releaseLockingTransaction.await()
            }
        }
        slotLocked.await()

        // when
        val result = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                admissionSlotRepositoryAdapter.findEmptyByRoomIdForUpdateSkipLocked(
                    Room.Id(roomId),
                    3
                )
            }
        }.await()

        // then
        try {
            assertEquals(
                listOf(2, 1),
                result.map { it.number }
            )
        } finally {
            releaseLockingTransaction.complete(Unit)
        }
        lockingTransaction.await()
    }

    @Test
    fun `첫 번째 빈 슬롯이 잠겨 있으면 다음 빈 슬롯을 반환한다`() = runTest {
        // given
        val roomId = "slot-join-001"
        val lockedSlotNumber = 2
        val transactionOperator = TransactionalOperator.create(
            R2dbcTransactionManager(connectionFactory)
        )
        val slotLocked = CompletableDeferred<Unit>()
        val releaseLockingTransaction = CompletableDeferred<Unit>()

        val lockingTransaction = async(Dispatchers.Default) {
            transactionOperator.executeAndAwait {
                databaseClient.sql(
                    """
                    select slot_number
                    from admission_slot
                    where room_id = :roomId
                        and slot_number = :slotNumber
                    for update
                    """.trimIndent()
                )
                    .bind("roomId", roomId)
                    .bind("slotNumber", lockedSlotNumber)
                    .fetch()
                    .one()
                    .awaitSingle()

                slotLocked.complete(Unit)
                releaseLockingTransaction.await()
            }
        }
        slotLocked.await()

        try {
            // when
            val result = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(2_000) {
                    async(Dispatchers.Default) {
                        transactionOperator.executeAndAwait {
                            admissionSlotRepositoryAdapter.findFirstEmptyByRoomIdForUpdateSkipLocked(
                                Room.Id(roomId)
                            )
                        }
                    }.await()
                }
            }

            // then
            assertEquals(3, result?.number)
        } finally {
            releaseLockingTransaction.complete(Unit)
        }
        lockingTransaction.await()
    }

}
