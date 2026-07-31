package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model.UserEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserEntityRepository : CoroutineCrudRepository<UserEntity, String> {

    @Modifying
    @Query(
        """
        insert into users(
            id,
            principal,
            password_hash,
            display_name,
            registered_at
        ) values (
            :#{#entity.id},
            :#{#entity.principal},
            :#{#entity.passwordHash},
            :#{#entity.displayName},
            current_timestamp
        )
    """
    )
    suspend fun insert(entity: UserEntity)

    @Query(
        """
        select exists(
            select 1
            from users
            where principal = :principal
              and withdrawn_at is null
        )
        """
    )
    suspend fun existsByPrincipal(principal: String): Boolean

}
