package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository

import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model.AuthenticationGrantEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AuthenticationGrantEntityRepository : CoroutineCrudRepository<AuthenticationGrantEntity, String> {

    @Modifying
    @Query(
        """
        insert into authentication_grant(
           id,
           user_id,
           issued_at,
           ac_jti,
           ac_issued_at,
           ac_will_expires_at,
           rc_id,
           rc_issued_at,
           rc_will_expires_at
        ) values (
            :#{#entity.id},
            :#{#entity.userId},
            :#{#entity.issuedAt},
            :#{#entity.acJti},
            :#{#entity.acIssuedAt},
            :#{#entity.acWillExpiresAt},
            :#{#entity.rcId},
            :#{#entity.rcIssuedAt},
            :#{#entity.rcWillExpiresAt}
        )
    """
    )
    suspend fun insert(entity: AuthenticationGrantEntity)

    @Query(
        """
        select
            id,
            user_id,
            issued_at,
            ac_jti,
            ac_issued_at,
            ac_will_expires_at,
            rc_id,
            rc_issued_at,
            rc_will_expires_at
        from authentication_grant
        where ac_jti = :acJti
        """
    )
    suspend fun findByAcJti(acJti: String): AuthenticationGrantEntity?

    @Query(
        """
        select
            id,
            user_id,
            issued_at,
            ac_jti,
            ac_issued_at,
            ac_will_expires_at,
            rc_id,
            rc_issued_at,
            rc_will_expires_at
        from authentication_grant
        where rc_id = :rcId
        for update
        """
    )
    suspend fun findByRcIdForUpdate(rcId: String): AuthenticationGrantEntity?

    @Modifying
    @Query(
        """
        delete
        from authentication_grant
        where ac_jti = :acJti
        """
    )
    suspend fun deleteByAcJti(acJti: String)

    @Modifying
    @Query(
        """
        delete
        from authentication_grant
        where rc_id = :rcId
        """
    )
    suspend fun deleteByRcId(rcId: String)

}
