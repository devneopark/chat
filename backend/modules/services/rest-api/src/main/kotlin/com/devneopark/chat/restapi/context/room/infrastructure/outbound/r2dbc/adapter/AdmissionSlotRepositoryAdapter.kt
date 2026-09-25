package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.admission_slot.reference.AdmissionSlotId
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository.AdmissionSlotEntityRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

private const val INSERT_ADMISSION_SLOT_SQL = """
    insert into admission_slot(
        room_id,
        slot_number,
        occupant_participant_id
    )
    values ($1, $2, $3)
"""

private const val INSERT_EMPTY_ADMISSION_SLOT_SQL = """
    insert into admission_slot(
        room_id,
        slot_number,
        occupant_participant_id
    )
    values ($1, $2, null)
"""

private const val DELETE_ADMISSION_SLOT_BY_ID_SQL = """
    delete from admission_slot as target
    using unnest($1::varchar[], $2::integer[]) as requested(room_id, slot_number)
    where target.room_id = requested.room_id
        and target.slot_number = requested.slot_number
"""

@Repository
class AdmissionSlotRepositoryAdapter(

    private val databaseClient: DatabaseClient,

    private val admissionSlotEntityRepository: AdmissionSlotEntityRepository

) : AdmissionSlotRepositoryPort {

    override suspend fun provision(roomId: RoomId, count: Int, participantId: ParticipantId) {
        databaseClient.inConnection { connection ->
            val statement = connection.createStatement(INSERT_ADMISSION_SLOT_SQL)

            repeat(count) { slotIndex ->
                if (slotIndex > 0) {
                    statement.add()
                }
                val slotNumber = slotIndex + 1
                statement.bind(0, roomId.value)
                statement.bind(1, slotNumber)

                if (slotNumber == 1) {
                    statement.bind(2, participantId.value)
                    return@repeat
                }

                statement.bindNull(2, String::class.java)
            }

            Flux.from(statement.execute())
                .flatMap { result -> result.rowsUpdated }
                .then()
        }.awaitSingleOrNull()
    }

    override suspend fun provisionAdditionalSlots(
        roomId: RoomId,
        currentMaxSlotNumber: Int,
        count: Int
    ) {
        if (count <= 0) {
            return
        }

        databaseClient.inConnection { connection ->
            val statement = connection.createStatement(INSERT_EMPTY_ADMISSION_SLOT_SQL)

            repeat(count) { index ->
                if (index > 0) {
                    statement.add()
                }
                statement.bind(0, roomId.value)
                statement.bind(1, currentMaxSlotNumber + index + 1)
            }

            Flux.from(statement.execute())
                .flatMap { result -> result.rowsUpdated }
                .then()
        }.awaitSingleOrNull()
    }

    override suspend fun countByRoomId(roomId: RoomId): Int {
        return admissionSlotEntityRepository.countByRoomId(roomId.value).toInt()
    }

    override suspend fun findMaxSlotNumberByRoomId(roomId: RoomId): Int {
        return admissionSlotEntityRepository.findMaxSlotNumberByRoomId(roomId.value)
    }

    override suspend fun findEmptyByRoomIdForUpdateSkipLocked(roomId: RoomId, limit: Int): List<AdmissionSlotId> {
        return admissionSlotEntityRepository
            .findEmptyByRoomIdForUpdateSkipLocked(roomId.value, limit)
            .map { entity -> entity.toDomain().id }
    }

    override suspend fun deleteAllByIds(slots: List<AdmissionSlotId>) {
        if (slots.isEmpty()) {
            return
        }

        databaseClient.inConnection { connection ->
            val statement = connection.createStatement(DELETE_ADMISSION_SLOT_BY_ID_SQL)

            statement.bind(0, slots.map { it.roomId }.toTypedArray())
            statement.bind(1, slots.map { it.number }.toTypedArray())

            Flux.from(statement.execute())
                .flatMap { result -> result.rowsUpdated }
                .then()
        }.awaitSingleOrNull()
    }

}
