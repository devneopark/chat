package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.room.service.RoomInfoValidator
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.DuplicatedTitleException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomInfoUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 채팅방 호스트의 정보 변경 요청을 실행하는 응용 서비스다. */
@Service
class UpdateRoomInfoService(

    private val roomInfoValidator: RoomInfoValidator,

    private val passwordHasher: PasswordHasher,

    private val roomRepositoryPort: RoomRepositoryPort,

    private val participantRepositoryPort: ParticipantRepositoryPort

) : UpdateRoomInfoUseCase {

    /** 활성 방과 호스트 권한을 확인한 뒤 제목과 비밀번호를 변경한다. */
    @Transactional
    override suspend fun update(command: UpdateRoomInfoUseCase.Command) {
        val roomId = Room.Id.from(command.roomId)
        val hostUserId = User.Id.from(command.hostUserId)
        val room = roomRepositoryPort.findActiveByIdForUpdate(roomId)
            ?: throw RoomNotFoundException()

        val isHost = participantRepositoryPort.existsActiveHostForRead(roomId, hostUserId)
        if (!isHost) {
            throw RoomNotFoundException()
        }

        roomInfoValidator.validateTitle(command.title)
        if (roomRepositoryPort.existsByTitleExceptRoomId(command.title, roomId)) {
            throw DuplicatedTitleException()
        }
        var passwordHash: String? = null
        if (command.rawPassword != null) {
            roomInfoValidator.validatePassword(command.rawPassword)
            passwordHash = passwordHasher.hash(command.rawPassword)
        }

        room.changeTitle(command.title)
        room.changePasswordHash(passwordHash)

        roomRepositoryPort.update(room)
    }

}
