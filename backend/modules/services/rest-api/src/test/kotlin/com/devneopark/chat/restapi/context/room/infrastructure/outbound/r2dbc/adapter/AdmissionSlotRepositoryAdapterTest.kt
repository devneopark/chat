package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.room.model.Room
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
import java.util.UUID
import kotlin.test.assertEquals

@DataR2dbcTest
@ActiveProfiles("test")
@Import(AdmissionSlotRepositoryAdapter::class)
class AdmissionSlotRepositoryAdapterTest {

    @Autowired
    lateinit var admissionSlotRepositoryAdapter: AdmissionSlotRepositoryAdapter

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
            registry.add("spring.sql.init.data-locations") {
                "classpath:init/AdmissionSlotRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `provision하면 호스트가 첫 번째 슬롯을 점유한 정원만큼 저장한다`() = runTest {
        // given
        val roomId = Room.Id("slot-room-001")
        val participantId = Participant.Id("slot-participant-001")

        // when
        admissionSlotRepositoryAdapter.provision(roomId, 3, participantId)

        // then
        val slots = databaseClient.sql(
            """
            select
                slot_number,
                occupant_participant_id
            from admission_slot
            where room_id = :roomId
            order by slot_number
            """.trimIndent()
        )
            .bind("roomId", roomId.value)
            .map { row ->
                row.get("slot_number", Int::class.javaObjectType)!! to
                    row.get("occupant_participant_id", String::class.java)
            }
            .all()
            .collectList()
            .awaitSingle()

        assertEquals(3, slots.size)
        assertEquals(1 to "slot-participant-001", slots[0])
        assertEquals(2 to null, slots[1])
        assertEquals(3 to null, slots[2])
    }

}
