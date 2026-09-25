package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.RoomParticipantNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.TransferRoomHostUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import com.devneopark.chat.restapi.shared.application.port.outbound.ApplicationLockPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val ROOM_PARTICIPANT_LOCK_KEY_PREFIX = "room:host:"

/** 채팅방 호스트 변경 요청을 실행하는 응용 서비스다. */
@Service
class TransferRoomHostService(

    private val roomRepositoryPort: RoomRepositoryPort,

    private val participantRepositoryPort: ParticipantRepositoryPort,

    private val applicationLockPort: ApplicationLockPort

) : TransferRoomHostUseCase {

    /** 활성 방과 요청자의 호스트 권한을 확인한 뒤 대상 참여자에게 호스트 역할을 이전한다. */
    @Transactional
    override suspend fun transfer(command: TransferRoomHostUseCase.Command) {
        val roomId = Room.Id.from(command.roomId)
        val requesterUserId = User.Id.from(command.requesterUserId)
        val targetUserId = User.Id.from(command.targetUserId)

        roomRepositoryPort.findActiveByIdForRead(roomId)
            ?: throw RoomNotFoundException()

        applicationLockPort.lock("$ROOM_PARTICIPANT_LOCK_KEY_PREFIX${roomId.value}")

        val participants = participantRepositoryPort
            .findActiveByRoomIdAndUserIdsForUpdate(roomId, requesterUserId, targetUserId)
            .associateBy { it.userId.value }

        val requester = participants[requesterUserId.value]
        if (requester == null || requester.role != ParticipantRole.HOST) {
            throw RoomNotFoundException()
        }

        val target = participants[targetUserId.value]
            ?: throw RoomParticipantNotFoundException()

        if (target.role != ParticipantRole.GUEST) {
            throw RoomParticipantNotFoundException()
        }

        requester.demoteToGuest()
        target.promoteToHost()

        participantRepositoryPort.updateRole(requester.id, requester.role)
        participantRepositoryPort.updateRole(target.id, target.role)
    }

}
