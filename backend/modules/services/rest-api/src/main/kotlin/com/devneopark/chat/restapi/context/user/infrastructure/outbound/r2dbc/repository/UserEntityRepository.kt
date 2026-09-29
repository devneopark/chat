package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model.UserEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/** 사용자 등록과 활성 사용자 조회, 사용자 상태·프로필 변경 쿼리를 제공하는 Spring Data R2DBC 저장소다. */
interface UserEntityRepository : CoroutineCrudRepository<UserEntity, String> {

    /** 사용자를 저장한다. */
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

    /** 활성 사용자 중 principal이 존재하는지 확인한다. */
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

    /** 탈퇴하지 않은 사용자를 식별자로 조회하고 쓰기 잠금을 획득한다. */
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
        for update
        """
    )
    suspend fun findByIdForUpdate(id: String): UserEntity?

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

    /** 탈퇴하지 않은 사용자를 탈퇴 상태로 변경한다. */
    @Modifying
    @Query(
        """
        update users
        set withdrawn_at = :withdrawnAt
        where id = :id
          and withdrawn_at is null
        """
    )
    suspend fun withdraw(id: String, withdrawnAt: java.time.Instant)

}
