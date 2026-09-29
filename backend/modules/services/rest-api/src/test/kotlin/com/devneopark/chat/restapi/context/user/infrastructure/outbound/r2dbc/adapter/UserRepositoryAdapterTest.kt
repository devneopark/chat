package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model.UserEntity
import com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.repository.UserEntityRepository
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.toKotlinInstant

@DataR2dbcTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
                "user.context.find.principal",
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
        assertTrue(userRepositoryAdapter.existsByPrincipal("user.context.find.principal"))
    }

    @Test
    fun `탈퇴한 principal 조회 결과는 false다`() = runTest {
        assertFalse(userRepositoryAdapter.existsByPrincipal("withdrawn.context.principal"))
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
            userEntityRepository.insert(entity)
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
            userEntityRepository.insert(entity)
        }

        assertTrue(
            generateSequence(exception as Throwable) { it.cause }
                .any { it.message?.contains("value too long") == true }
        )
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
    fun `id로 조회하면서 쓰기 잠금을 획득하면 User로 변환한다`() = runTest {
        val result = userRepositoryAdapter.findByIdForUpdate(User.Id("user-context-find-001"))

        assertNotNull(result)
        assertEquals("user-context-find-001", result.id.value)
    }

    @Test
    fun `id에 해당하는 사용자가 없으면 쓰기 잠금 조회도 null을 반환한다`() = runTest {
        val result = userRepositoryAdapter.findByIdForUpdate(User.Id("user-context-missing-001"))

        assertNull(result)
    }

    @Test
    fun `id에 해당하는 사용자가 탈퇴 상태면 쓰기 잠금 조회도 null을 반환한다`() = runTest {
        val result = userRepositoryAdapter.findByIdForUpdate(User.Id("user-context-withdrawn-001"))

        assertNull(result)
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

    @Test
    fun `활성 사용자를 탈퇴 상태로 변경한다`() = runTest {
        // given
        val userId = User.Id("user-context-update-001")
        val withdrawnAt = Instant.parse("2026-09-28T00:00:00Z")

        // when
        userRepositoryAdapter.withdraw(userId, withdrawnAt.toKotlinInstant())

        // then
        val result = databaseClient.sql(
            "select withdrawn_at from users where id = :id"
        )
            .bind("id", userId.value)
            .map { row -> row.get("withdrawn_at", Instant::class.java)!! }
            .one()
            .awaitSingle()

        assertEquals(withdrawnAt, result)
        assertNull(userRepositoryAdapter.findById(userId))
    }

    @Test
    fun `탈퇴한 사용자를 다시 탈퇴 처리해도 기존 탈퇴 시각을 유지한다`() = runTest {
        // given
        val userId = User.Id("user-context-withdrawn-001")
        val before = databaseClient.sql(
            "select withdrawn_at from users where id = :id"
        )
            .bind("id", userId.value)
            .map { row -> row.get("withdrawn_at", Instant::class.java)!! }
            .one()
            .awaitSingle()

        // when
        userRepositoryAdapter.withdraw(
            userId,
            Instant.parse("2026-09-28T00:00:00Z").toKotlinInstant()
        )

        // then
        val after = databaseClient.sql(
            "select withdrawn_at from users where id = :id"
        )
            .bind("id", userId.value)
            .map { row -> row.get("withdrawn_at", Instant::class.java)!! }
            .one()
            .awaitSingle()

        assertEquals(before, after)
    }

    @Test
    fun `존재하지 않는 사용자를 탈퇴 처리해도 오류가 발생하지 않는다`() = runTest {
        userRepositoryAdapter.withdraw(
            User.Id("user-context-missing-001"),
            Instant.parse("2026-09-28T00:00:00Z").toKotlinInstant()
        )

        assertNull(userRepositoryAdapter.findById(User.Id("user-context-missing-001")))
    }

}
