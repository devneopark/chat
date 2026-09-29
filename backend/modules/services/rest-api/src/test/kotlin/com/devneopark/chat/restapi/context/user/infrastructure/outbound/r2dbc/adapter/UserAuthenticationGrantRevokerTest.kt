package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataR2dbcTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(UserAuthenticationGrantRevoker::class)
class UserAuthenticationGrantRevokerTest {

    @Autowired
    lateinit var userAuthenticationGrantRevoker: UserAuthenticationGrantRevoker

    @Autowired
    lateinit var databaseClient: DatabaseClient

    companion object {

        private val schemaName = "rest_api_" + UUID.randomUUID()
            .toString()
            .replace("-", "")

        @JvmStatic
        @DynamicPropertySource
        fun setProperties(registry: DynamicPropertyRegistry) {
            val db = SharedPostgresContainer.container
            DriverManager.getConnection(
                db.jdbcUrl,
                db.username,
                db.password
            ).use {
                it.createStatement().use {
                    it.execute("create schema if not exists \"${schemaName}\"")
                }
            }
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${db.host}:${db.firstMappedPort}/${db.databaseName}?schema=${schemaName}"
            }
            registry.add("spring.r2dbc.username", db::getUsername)
            registry.add("spring.r2dbc.password", db::getPassword)
            registry.add("spring.sql.init.schema-locations") {
                "classpath:init/UserAuthenticationGrantRevokerTest-schema.sql"
            }
            registry.add("spring.sql.init.data-locations") {
                "classpath:init/UserAuthenticationGrantRevokerTest.sql"
            }
        }

    }

    @Test
    fun `사용자 식별자로 인증정보를 모두 폐기한다`() = runTest {
        // when
        userAuthenticationGrantRevoker.revokeAllByUserId(User.Id("seed-user-001"))
        userAuthenticationGrantRevoker.revokeAllByUserId(User.Id("seed-user-001"))

        // then
        val remainingCount = databaseClient.sql(
            "select count(*) as count from authentication_grant where user_id = :userId"
        )
            .bind("userId", "seed-user-001")
            .map { row -> row.get("count", Long::class.java)!! }
            .one()
            .awaitSingle()

        assertEquals(0L, remainingCount)

        val otherUserCount = databaseClient.sql(
            "select count(*) as count from authentication_grant where user_id = :userId"
        )
            .bind("userId", "other-user-001")
            .map { row -> row.get("count", Long::class.java)!! }
            .one()
            .awaitSingle()

        assertNotNull(otherUserCount)
        assertEquals(1L, otherUserCount)
    }

}
