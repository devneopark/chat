package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.iam.application.port.outbound.UserRepositoryPort
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model.UserEntity
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository.UserEntityRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(

    private val userEntityRepository: UserEntityRepository

) : UserRepositoryPort {

    override suspend fun insert(user: User): User {
        val entity = UserEntity.from(user)
        userEntityRepository.insert(entity)
        return user
    }


    override suspend fun existsByPrincipal(principal: String): Boolean {
        return userEntityRepository.existsByPrincipal(principal)
    }

}