package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DataR2dbcTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(UserActiveRoomParticipationChecker::class)
class UserActiveRoomParticipationCheckerTest {

    @Autowired
    lateinit var userActiveRoomParticipationChecker: UserActiveRoomParticipationChecker

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
                "classpath:init/UserActiveRoomParticipationCheckerTest.sql"
            }
        }

    }

    @Test
    fun `활성 채팅방에 참여 중인 사용자는 true를 반환한다`() = runTest {
        assertTrue(
            userActiveRoomParticipationChecker.existsByUserId(
                User.Id("active-room-user-001")
            )
        )
    }

    @Test
    fun `퇴장한 채팅방만 남은 사용자는 false를 반환한다`() = runTest {
        assertFalse(
            userActiveRoomParticipationChecker.existsByUserId(
                User.Id("active-room-user-002")
            )
        )
    }

    @Test
    fun `참여 이력이 없는 사용자는 false를 반환한다`() = runTest {
        assertFalse(
            userActiveRoomParticipationChecker.existsByUserId(
                User.Id("active-room-user-003")
            )
        )
    }

    @Test
    fun `퇴장 이력과 현재 참여가 함께 있는 사용자는 true를 반환한다`() = runTest {
        assertTrue(
            userActiveRoomParticipationChecker.existsByUserId(
                User.Id("active-room-user-004")
            )
        )
    }

}
