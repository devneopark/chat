package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.RoomParticipantNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.LeaveRoomUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

/** 활성 채팅방에서 사용자를 퇴장시키고 필요한 호스트 승계 또는 방 폐쇄를 수행하는 응용 서비스다. */
@Service
class LeaveRoomService(

    private val roomRepositoryPort: RoomRepositoryPort,

    private val participantRepositoryPort: ParticipantRepositoryPort,

    private val admissionSlotRepositoryPort: AdmissionSlotRepositoryPort,

    private val clock: Clock

) : LeaveRoomUseCase {

    /** 활성 방과 참여자를 잠근 뒤 참여자·슬롯 상태를 정리하고 호스트 퇴장 시 후속 상태를 처리한다. */
    @Transactional
    override suspend fun leave(command: LeaveRoomUseCase.Command) {
        val roomId = Room.Id.from(command.roomId)
        val userId = User.Id.from(command.userId)

        roomRepositoryPort.findActiveByIdForUpdate(roomId)
            ?: throw RoomNotFoundException()

        val participant = participantRepositoryPort
            .findActiveByRoomIdAndUserIdForUpdate(roomId, userId)
            ?: throw RoomParticipantNotFoundException()

        val isHost = ParticipantRole.HOST == participant.role
        var hostCandidate: Participant? = null
        if (isHost) {
            hostCandidate = participantRepositoryPort.findOldestActiveGuestForUpdate(roomId)
        }
        val exitedAt = clock.instant().toKotlinInstant()

        participantRepositoryPort.markExited(participant.id, exitedAt)
        admissionSlotRepositoryPort.releaseParticipant(participant.id)

        if (!isHost) {
            return
        }

        if (hostCandidate == null) {
            roomRepositoryPort.close(roomId, exitedAt)
            return
        }

        participantRepositoryPort.updateRole(hostCandidate.id, ParticipantRole.HOST)
    }

}
