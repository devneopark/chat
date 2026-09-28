package com.devneopark.chat.restapi.context.user.application.service

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.user.application.exception.UserHasActiveRoomException
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.WithdrawUserUseCase
import com.devneopark.chat.restapi.context.user.application.port.outbound.ActiveRoomParticipationChecker
import com.devneopark.chat.restapi.context.user.application.port.outbound.AuthenticationGrantRevoker
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

/** 활성 채팅방 참여 여부를 확인하고 사용자의 탈퇴를 처리하는 응용 서비스다. */
@Service
class WithdrawUserService(

    private val userRepositoryPort: UserRepositoryPort,

    private val activeRoomParticipationChecker: ActiveRoomParticipationChecker,

    private val authenticationGrantRevoker: AuthenticationGrantRevoker,

    private val clock: Clock

) : WithdrawUserUseCase {

    /** 활성 채팅방 참여 여부를 확인한 뒤 사용자를 탈퇴 처리하고 인증정보를 정리한다. */
    @Transactional
    override suspend fun withdraw(command: WithdrawUserUseCase.Command) {
        val userId = User.Id.from(command.userId)

        userRepositoryPort.findByIdForUpdate(userId)
            ?: throw UserNotFoundException()

        if (activeRoomParticipationChecker.existsByUserId(userId)) {
            throw UserHasActiveRoomException()
        }

        val withdrawnAt = clock.instant().toKotlinInstant()
        userRepositoryPort.withdraw(userId, withdrawnAt)
        authenticationGrantRevoker.revokeAllByUserId(userId)
    }

}
