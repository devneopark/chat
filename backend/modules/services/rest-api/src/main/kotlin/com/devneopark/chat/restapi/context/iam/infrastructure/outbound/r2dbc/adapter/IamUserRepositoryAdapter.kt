package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.iam.application.port.outbound.IamUserRepositoryPort
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository.IamUserEntityRepository
import org.springframework.stereotype.Repository

@Repository
class IamUserRepositoryAdapter(

    private val iamUserEntityRepository: IamUserEntityRepository

) : IamUserRepositoryPort {

    override suspend fun findByPrincipal(principal: String): User? {
        return iamUserEntityRepository.findByPrincipal(principal)
            ?.toDomain()
    }

}
