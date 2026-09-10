package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository.UserEntityRepository
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataR2dbcTest
@ActiveProfiles("test")
@Import(UserRepositoryAdapter::class)
class UserRepositoryAdapterTest {

    @Autowired
    lateinit var userRepositoryAdapter: UserRepositoryAdapter

    @Autowired
    lateinit var userEntityRepository: UserEntityRepository

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
            registry.add("spring.sql.init.data-locations") {
                "classpath:init/UserContextUserRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `id로 조회하면 User로 변환한다`() = runTest {
        val result = userRepositoryAdapter.findById(User.Id("user-context-find-001"))

        assertNotNull(result)
        assertEquals("user-context-find-001", result.id.value)
        assertEquals("user.context.find.principal", result.credential.principal)
        assertEquals("find-hashed-password", result.credential.passwordHash)
        assertEquals("Seed User", result.profile.displayName)
    }

    @Test
    fun `id에 해당하는 사용자가 없으면 null을 반환한다`() = runTest {
        val result = userRepositoryAdapter.findById(User.Id("user-context-missing-001"))

        assertNull(result)
    }

    @Test
    fun `id에 해당하는 사용자가 탈퇴 상태면 null을 반환한다`() = runTest {
        val result = userRepositoryAdapter.findById(User.Id("user-context-withdrawn-001"))

        assertNull(result)
    }

    @Test
    fun `사용자 프로필을 갱신하면 displayName만 변경된다`() = runTest {
        val user = User(
            User.Id("user-context-update-001"),
            Credential("user.context.update.principal", "update-hashed-password"),
            Profile("Updated User")
        )

        userRepositoryAdapter.updateProfile(user)

        val entity = userEntityRepository.findById(user.id.value)

        assertNotNull(entity)
        assertEquals("Updated User", entity.displayName)
        assertEquals("user.context.update.principal", entity.principal)
        assertEquals("update-hashed-password", entity.passwordHash)
    }

    @Test
    fun `탈퇴한 사용자의 프로필은 갱신하지 않는다`() = runTest {
        val user = User(
            User.Id("user-context-withdrawn-001"),
            Credential("withdrawn.context.principal", "withdrawn-hashed-password"),
            Profile("Should Not Update")
        )

        userRepositoryAdapter.updateProfile(user)

        val displayName = databaseClient.sql(
            "select display_name from users where id = :id"
        )
            .bind("id", user.id.value)
            .map { row -> row.get("display_name", String::class.java) ?: "" }
            .one()
            .awaitSingle()

        assertEquals("Withdrawn User", displayName)
    }

}
