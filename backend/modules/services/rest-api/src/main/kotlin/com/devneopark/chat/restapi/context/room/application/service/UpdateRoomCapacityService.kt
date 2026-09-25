package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.CapacityReductionUnavailableException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.policy.AdmissionSlotPolicy
import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomCapacityUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import com.devneopark.chat.restapi.shared.application.port.outbound.ApplicationLockPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val ROOM_CAPACITY_LOCK_KEY_PREFIX = "room:capacity:"

/** 채팅방 호스트의 정원 변경 요청을 실행하는 응용 서비스다. */
@Service
class UpdateRoomCapacityService(

    private val admissionSlotPolicy: AdmissionSlotPolicy,

    private val roomRepositoryPort: RoomRepositoryPort,

    private val participantRepositoryPort: ParticipantRepositoryPort,

    private val admissionSlotRepositoryPort: AdmissionSlotRepositoryPort,

    private val applicationLockPort: ApplicationLockPort

) : UpdateRoomCapacityUseCase {

    /** 활성 방과 호스트 권한을 확인한 뒤 목표 정원에 맞게 슬롯을 변경한다. */
    @Transactional
    override suspend fun update(command: UpdateRoomCapacityUseCase.Command) {
        val roomId = Room.Id.from(command.roomId)
        val hostUserId = User.Id.from(command.hostUserId)

        roomRepositoryPort.findActiveByIdForRead(roomId)
            ?: throw RoomNotFoundException()

        val isHost = participantRepositoryPort.existsActiveHostForRead(roomId, hostUserId)
        if (!isHost) {
            throw RoomNotFoundException()
        }

        admissionSlotPolicy.validateCapacity(command.newCapacity)
        applicationLockPort.lock("$ROOM_CAPACITY_LOCK_KEY_PREFIX${roomId.value}")

        val currentCapacity = admissionSlotRepositoryPort.countByRoomId(roomId)
        val delta = command.newCapacity - currentCapacity

        if (delta > 0) {
            val currentMaxSlotNumber = admissionSlotRepositoryPort.findMaxSlotNumberByRoomId(roomId)
            admissionSlotRepositoryPort.provisionAdditionalSlots(roomId, currentMaxSlotNumber, delta)
            return
        }

        if (delta < 0) {
            val requiredReleaseCount = -delta
            val removableSlots =
                admissionSlotRepositoryPort.findEmptyByRoomIdForUpdateSkipLocked(roomId, requiredReleaseCount)

            if (removableSlots.size != requiredReleaseCount) {
                throw CapacityReductionUnavailableException()
            }

            admissionSlotRepositoryPort.deleteAllByIds(removableSlots)
        }
    }

}
