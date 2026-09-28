package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.restapi.context.user.application.port.outbound.ActiveRoomParticipationChecker
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

private const val EXISTS_ACTIVE_ROOM_PARTICIPATION_SQL = """
    select exists(
        select 1
        from participant
        where user_id = :userId
          and exited_at is null
    ) as exists_active_participation
"""

/** participant 테이블에서 사용자의 활성 채팅방 참여 여부를 조회하는 구현체다. */
@Repository
class UserActiveRoomParticipationChecker(

    private val databaseClient: DatabaseClient

) : ActiveRoomParticipationChecker {

    /** 사용자의 활성 participant 레코드 존재 여부를 조회한다. */
    override suspend fun existsByUserId(userId: UserId): Boolean {
        return databaseClient.sql(EXISTS_ACTIVE_ROOM_PARTICIPATION_SQL)
            .bind("userId", userId.value)
            .map { row, _ -> row.get("exists_active_participation", Boolean::class.java) ?: false }
            .one()
            .awaitSingle()
    }

}
