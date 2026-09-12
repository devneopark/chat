package com.devneopark.chat.restapi.context.user.application.service

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.UpdateUserProfileUseCase
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 사용자 프로필 수정 유즈케이스를 실행하는 응용 서비스다. */
@Service
class UpdateUserProfileService(

    private val userRepositoryPort: UserRepositoryPort

) : UpdateUserProfileUseCase {

    /** 사용자를 조회하고, 변경이 필요한 경우 도메인 규칙을 거쳐 프로필을 저장한다. */
    @Transactional
    override suspend fun update(command: UpdateUserProfileUseCase.Command) {
        val userId = User.Id.from(command.userId)
        val user = userRepositoryPort.findById(userId)
            ?: run {
                throw UserNotFoundException()
            }
        if (user.profile.displayName == command.displayName) {
            return
        }
        user.changeDisplayName(command.displayName)
        userRepositoryPort.updateProfile(user)
    }

}
