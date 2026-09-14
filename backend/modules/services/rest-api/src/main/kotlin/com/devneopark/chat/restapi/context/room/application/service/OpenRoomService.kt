package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.room.service.RoomInfoValidator
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.room.application.exception.DuplicatedTitleException
import com.devneopark.chat.restapi.context.room.application.policy.AdmissionSlotPolicy
import com.devneopark.chat.restapi.context.room.application.port.inbound.OpenRoomUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

@Service
class OpenRoomService(

    private val roomInfoValidator: RoomInfoValidator,

    private val passwordHasher: PasswordHasher,

    private val admissionSlotPolicy: AdmissionSlotPolicy,

    private val roomRepositoryPort: RoomRepositoryPort,

    private val idGenerator: IdGenerator,

    private val clock: Clock,

    private val participantRepositoryPort: ParticipantRepositoryPort,

    private val admissionSlotRepositoryPort: AdmissionSlotRepositoryPort

) : OpenRoomUseCase {

    @Transactional
    override suspend fun open(command: OpenRoomUseCase.Command): OpenRoomUseCase.Result {
        roomInfoValidator.validateTitle(command.title)
        var passwordHash: String? = null
        if (command.rawPassword != null) {
            roomInfoValidator.validatePassword(command.rawPassword)
            passwordHash = passwordHasher.hash(command.rawPassword)
        }
        admissionSlotPolicy.validateCapacity(command.capacity)

        val isTitleDuplicated = roomRepositoryPort.existsByTitle(command.title)
        if (isTitleDuplicated) {
            throw DuplicatedTitleException()
        }

        val roomIdValue = idGenerator.generate()
        val roomId = Room.Id(roomIdValue)
        val room = Room(roomId, command.title, passwordHash)
        roomRepositoryPort.insert(room)

        val participantIdValue = idGenerator.generate()
        val participantId = Participant.Id(participantIdValue)
        val userId = User.Id.from(command.hostUserId)
        val participant = Participant(
            participantId,
            roomId,
            userId,
            ParticipantRole.HOST,
            clock.instant().toKotlinInstant()
        )
        participantRepositoryPort.insert(participant)

        admissionSlotRepositoryPort.provision(roomId, command.capacity, participantId)

        return OpenRoomUseCase.Result(roomIdValue)
    }

}
