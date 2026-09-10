package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model.UserEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserEntityRepository : CoroutineCrudRepository<UserEntity, String> {

    @Query(
        """
        select
            id,
            principal,
            password_hash,
            display_name
        from users
        where id = :id
          and withdrawn_at is null
        """
    )
    override suspend fun findById(id: String): UserEntity?

    @Modifying
    @Query(
        """
        update users
        set display_name = :#{#user.displayName}
        where id = :#{#user.id}
          and withdrawn_at is null
        """
    )
    suspend fun updateUserProfile(user: UserEntity)

}
