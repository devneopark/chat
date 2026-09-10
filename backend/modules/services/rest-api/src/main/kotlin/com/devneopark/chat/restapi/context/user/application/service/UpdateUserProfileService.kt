package com.devneopark.chat.restapi.context.user.application.service

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.UpdateUserProfileUseCase
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateUserProfileService(

    private val userRepositoryPort: UserRepositoryPort

) : UpdateUserProfileUseCase {

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