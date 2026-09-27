package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.admission_slot.model.AdmissionSlot
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

    @Test
    fun `현재 방의 슬롯 수를 반환한다`() = runTest {
        // when
        val result = admissionSlotRepositoryAdapter.countByRoomId(Room.Id("slot-count-001"))

        // then
        assertEquals(2, result)
    }

    @Test
    fun `현재 방의 가장 큰 슬롯 번호를 반환한다`() = runTest {
        // when
        val result = admissionSlotRepositoryAdapter.findMaxSlotNumberByRoomId(
            Room.Id("slot-additional-001")
        )

        // then
        assertEquals(3, result)
    }

    @Test
    fun `추가 슬롯을 증설하면 현재 최대 번호 다음부터 빈 슬롯을 저장한다`() = runTest {
        // given
        val roomId = Room.Id("slot-additional-001")

        // when
        admissionSlotRepositoryAdapter.provisionAdditionalSlots(roomId, 3, 2)

        // then
        val slotNumbers = databaseClient.sql(
            """
            select slot_number
            from admission_slot
            where room_id = :roomId
            order by slot_number
            """.trimIndent()
        )
            .bind("roomId", roomId.value)
            .map { row -> row.get("slot_number", Int::class.javaObjectType)!! }
            .all()
            .collectList()
            .awaitSingle()

        assertEquals(listOf(1, 3, 4, 5), slotNumbers)
    }

    @Test
    fun `빈 슬롯을 잠금 조회하면 번호가 큰 순서로 제한된 수만 반환한다`() = runTest {
        // when
        val result = admissionSlotRepositoryAdapter.findEmptyByRoomIdForUpdateSkipLocked(
            Room.Id("slot-empty-001"),
            2
        )

        // then
        assertEquals(
            listOf(
                "slot-empty-001" to 3,
                "slot-empty-001" to 2
            ),
            result.map { it.roomId to it.number }
        )
    }

    @Test
    fun `첫 번째 빈 슬롯을 잠금 조회하면 번호가 작은 슬롯을 반환한다`() = runTest {
        // when
        val result = admissionSlotRepositoryAdapter.findFirstEmptyByRoomIdForUpdateSkipLocked(
            Room.Id("slot-join-001")
        )

        // then
        assertEquals("slot-join-001" to 2, result?.roomId to result?.number)
    }

    @Test
    fun `빈 슬롯이 없으면 첫 번째 빈 슬롯 잠금 조회가 null을 반환한다`() = runTest {
        // when
        val result = admissionSlotRepositoryAdapter.findFirstEmptyByRoomIdForUpdateSkipLocked(
            Room.Id("slot-no-empty-001")
        )

        // then
        assertEquals(null, result)
    }

    @Test
    fun `슬롯을 점유하면 참여자 식별자를 저장한다`() = runTest {
        // given
        val slotId = AdmissionSlot.Id("slot-assign-001", 1)
        val participantId = Participant.Id("slot-participant-003")

        // when
        admissionSlotRepositoryAdapter.assignParticipant(slotId, participantId)

        // then
        val occupantParticipantId = databaseClient.sql(
            """
            select occupant_participant_id
            from admission_slot
            where room_id = :roomId
                and slot_number = :slotNumber
            """.trimIndent()
        )
            .bind("roomId", slotId.roomId)
            .bind("slotNumber", slotId.number)
            .map { row -> row.get("occupant_participant_id", String::class.javaObjectType)!! }
            .one()
            .awaitSingle()

        assertEquals(participantId.value, occupantParticipantId)
    }

    @Test
    fun `이미 점유된 슬롯은 다른 참여자로 덮어쓰지 않는다`() = runTest {
        // given
        val slotId = AdmissionSlot.Id("slot-join-001", 1)
        val existingParticipantId = Participant.Id("slot-participant-002")
        val newParticipantId = Participant.Id("slot-participant-003")

        // when
        admissionSlotRepositoryAdapter.assignParticipant(slotId, newParticipantId)

        // then
        val occupantParticipantId = databaseClient.sql(
            """
            select occupant_participant_id
            from admission_slot
            where room_id = :roomId
                and slot_number = :slotNumber
            """.trimIndent()
        )
            .bind("roomId", slotId.roomId)
            .bind("slotNumber", slotId.number)
            .map { row -> row.get("occupant_participant_id", String::class.javaObjectType)!! }
            .one()
            .awaitSingle()

        assertEquals(existingParticipantId.value, occupantParticipantId)
    }

    @Test
    fun `슬롯 식별자 목록으로 슬롯을 일괄 삭제한다`() = runTest {
        // given
        val roomId = Room.Id("slot-delete-001")
        val slots = listOf(
            AdmissionSlot.Id(roomId.value, 1),
            AdmissionSlot.Id(roomId.value, 3)
        )

        // when
        admissionSlotRepositoryAdapter.deleteAllByIds(slots)

        // then
        val remainingSlotNumbers = databaseClient.sql(
            """
            select slot_number
            from admission_slot
            where room_id = :roomId
            order by slot_number
            """.trimIndent()
        )
            .bind("roomId", roomId.value)
            .map { row -> row.get("slot_number", Int::class.javaObjectType)!! }
            .all()
            .collectList()
            .awaitSingle()

        assertEquals(listOf(2), remainingSlotNumbers)
    }

}
