package com.devneopark.chat.restapi.shared.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.restapi.shared.application.port.outbound.ApplicationLockPort
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

private const val ADVISORY_LOCK_SQL = """
    select pg_advisory_xact_lock(hashtextextended(:key, 0))
"""

@Repository
class PostgresAdvisoryLockAdapter(

    private val databaseClient: DatabaseClient

) : ApplicationLockPort {

    override suspend fun lock(key: String) {
        databaseClient.sql(ADVISORY_LOCK_SQL)
            .bind("key", key)
            .fetch()
            .one()
            .awaitSingleOrNull()
    }

}
