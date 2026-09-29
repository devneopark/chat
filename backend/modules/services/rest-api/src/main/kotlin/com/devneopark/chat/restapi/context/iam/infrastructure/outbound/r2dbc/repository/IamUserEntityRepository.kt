package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model.UserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface IamUserEntityRepository : CoroutineCrudRepository<UserEntity, String> {

    @Query(
        """
        select
            id,
            principal,
            password_hash,
            display_name
        from users
        where principal = :principal
          and withdrawn_at is null
        """
    )
    suspend fun findByPrincipal(principal: String): UserEntity?

}
