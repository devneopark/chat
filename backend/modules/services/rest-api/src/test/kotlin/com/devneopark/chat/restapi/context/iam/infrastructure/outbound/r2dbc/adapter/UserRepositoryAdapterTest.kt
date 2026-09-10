package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model.UserEntity
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository.IamUserEntityRepository
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@DataR2dbcTest
@ActiveProfiles("test")
@Import(IamUserRepositoryAdapter::class)
class UserRepositoryAdapterTest {

    @Autowired
    lateinit var userRepositoryAdapter: IamUserRepositoryAdapter

    @Autowired
    lateinit var iamUserEntityRepository: IamUserEntityRepository

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
                "classpath:init/UserRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `insert하면 실제 저장되고 입력한 user를 반환한다`() = runTest {
        val user = User(
            User.Id("insert-user-001"),
            Credential(
                "insert.principal",
                "hashed-password"
            ),
            Profile("Neo")
        )

        val result = userRepositoryAdapter.insert(user)

        assertSame(user, result)
        assertTrue(userRepositoryAdapter.existsByPrincipal(user.credential.principal))
    }

    @Test
    fun `insert 시 principal 중복 제약이 동작한다`() = runTest {
        val user = User(
            User.Id("duplicate-user-001"),
            Credential(
                "existing.principal",
                "hashed-password"
            ),
            Profile("Duplicate")
        )

        assertFailsWith<DataIntegrityViolationException> {
            userRepositoryAdapter.insert(user)
        }
    }

    @Test
    fun `이미 존재하는 principal 조회 결과는 true다`() = runTest {
        assertTrue(userRepositoryAdapter.existsByPrincipal("existing.principal"))
    }

    @Test
    fun `탈퇴한 principal 조회 결과는 false다`() = runTest {
        assertFalse(userRepositoryAdapter.existsByPrincipal("withdrawn.principal"))
    }

    @Test
    fun `사용 가능한 principal 조회 결과는 false다`() = runTest {
        assertFalse(userRepositoryAdapter.existsByPrincipal("available.principal"))
    }

    @Test
    fun `id가 컬럼 길이 제한을 초과하면 저장에 실패한다`() = runTest {
        val user = User(
            User.Id("i".repeat(33)),
            Credential(
                "id-limit.principal",
                "hashed-password"
            ),
            Profile("Id Limit")
        )

        val exception = assertFailsWith<DataAccessException> {
            userRepositoryAdapter.insert(user)
        }

        assertTrue(
            generateSequence(exception as Throwable) { it.cause }
                .any { it.message?.contains("value too long") == true }
        )
    }

    @Test
    fun `principal이 컬럼 길이 제한을 초과하면 저장에 실패한다`() = runTest {
        val entity = UserEntity().apply {
            id = "principal-limit-user"
            principal = "p".repeat(51)
            passwordHash = "hashed-password"
            displayName = "Principal Limit"
        }

        val exception = assertFailsWith<DataAccessException> {
            iamUserEntityRepository.insert(entity)
        }

        assertTrue(
            generateSequence(exception as Throwable) { it.cause }
                .any { it.message?.contains("value too long") == true }
        )
    }

    @Test
    fun `displayName이 컬럼 길이 제한을 초과하면 저장에 실패한다`() = runTest {
        val entity = UserEntity().apply {
            id = "display-name-limit-user"
            principal = "display-name-limit.principal"
            passwordHash = "hashed-password"
            displayName = "d".repeat(51)
        }

        val exception = assertFailsWith<DataAccessException> {
            iamUserEntityRepository.insert(entity)
        }

        assertTrue(
            generateSequence(exception as Throwable) { it.cause }
                .any { it.message?.contains("value too long") == true }
        )
    }

}
