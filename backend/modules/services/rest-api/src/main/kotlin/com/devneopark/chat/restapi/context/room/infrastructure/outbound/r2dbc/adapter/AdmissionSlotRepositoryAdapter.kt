package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
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

@Repository
class AdmissionSlotRepositoryAdapter(

    private val databaseClient: DatabaseClient

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

}
