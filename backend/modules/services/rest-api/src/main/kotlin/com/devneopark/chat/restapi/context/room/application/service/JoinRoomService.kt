package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.room.application.exception.AlreadyJoinedRoomException
import com.devneopark.chat.restapi.context.room.application.exception.RoomFullException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.WrongRoomPasswordException
import com.devneopark.chat.restapi.context.room.application.port.inbound.JoinRoomUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

/** 활성 채팅방에 사용자를 빈 슬롯과 함께 등록하는 응용 서비스다. */
@Service
class JoinRoomService(

    private val roomRepositoryPort: RoomRepositoryPort,

    private val participantRepositoryPort: ParticipantRepositoryPort,

    private val admissionSlotRepositoryPort: AdmissionSlotRepositoryPort,

    private val passwordHasher: PasswordHasher,

    private val idGenerator: IdGenerator,

    private val clock: Clock

) : JoinRoomUseCase {

    /** 활성 방과 비밀번호를 확인한 뒤 요청 사용자를 가장 작은 빈 슬롯에 배정한다. */
    @Transactional
    override suspend fun join(command: JoinRoomUseCase.Command) {
        val roomId = Room.Id.from(command.roomId)
        val userId = User.Id.from(command.userId)
        val room = roomRepositoryPort.findActiveByIdForRead(roomId)
            ?: throw RoomNotFoundException()

        val existingParticipant = participantRepositoryPort
            .findActiveByRoomIdAndUserIdForRead(roomId, userId)
        if (existingParticipant != null) {
            throw AlreadyJoinedRoomException()
        }

        val passwordHash = room.passwordHash
        if (passwordHash != null) {
            val rawPassword = command.rawPassword
                ?: throw WrongRoomPasswordException()
            if (!passwordHasher.matches(rawPassword, passwordHash)) {
                throw WrongRoomPasswordException()
            }
        }

        val slotId = admissionSlotRepositoryPort
            .findFirstEmptyByRoomIdForUpdateSkipLocked(roomId)
            ?: throw RoomFullException()

        val participant = Participant(
            Participant.Id(idGenerator.generate()),
            roomId,
            userId,
            ParticipantRole.GUEST,
            clock.instant().toKotlinInstant()
        )
        participantRepositoryPort.insert(participant)
        admissionSlotRepositoryPort.assignParticipant(slotId, participant.id)
    }

}
