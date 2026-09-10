package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model.UserEntity
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository.UserEntityRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(

    private val userEntityRepository: UserEntityRepository

) : UserRepositoryPort {

    override suspend fun findById(id: UserId): User? {
        return userEntityRepository.findById(id.value)
            ?.toDomain()
    }

    override suspend fun updateProfile(user: User) {
        val entity = UserEntity.from(user)
        userEntityRepository.updateUserProfile(entity)
    }

}