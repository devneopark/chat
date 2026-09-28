package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.restapi.context.user.application.port.outbound.AuthenticationGrantRevoker
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

private const val REVOKE_ALL_AUTHENTICATION_GRANTS_SQL = """
    delete
    from authentication_grant
    where user_id = :userId
"""

/** 사용자의 인증정보를 authentication_grant 테이블에서 일괄 삭제한다. */
@Repository
class UserAuthenticationGrantRevoker(

    private val databaseClient: DatabaseClient

) : AuthenticationGrantRevoker {

    /** 사용자에게 발급된 모든 인증정보를 삭제한다. */
    override suspend fun revokeAllByUserId(userId: UserId) {
        databaseClient.sql(REVOKE_ALL_AUTHENTICATION_GRANTS_SQL)
            .bind("userId", userId.value)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

}
