package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model.UserEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/** 활성 사용자 조회와 프로필 변경 쿼리를 제공하는 Spring Data R2DBC 저장소다. */
interface UserEntityRepository : CoroutineCrudRepository<UserEntity, String> {

    /** 탈퇴하지 않은 사용자만 식별자로 조회한다. */
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

    /** 탈퇴하지 않은 사용자의 표시 이름만 변경한다. */
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
