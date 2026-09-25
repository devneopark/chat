package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository.RoomEntityRepository
import com.devneopark.chat.restapi.shared.infrastructure.SharedPostgresContainer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@DataR2dbcTest
@ActiveProfiles("test")
@Import(RoomRepositoryAdapter::class)
class RoomRepositoryAdapterTest {

    @Autowired
    lateinit var roomRepositoryAdapter: RoomRepositoryAdapter

    @Autowired
    lateinit var roomEntityRepository: RoomEntityRepository

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
                "classpath:init/RoomRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `insert하면 실제 저장되고 입력한 room을 반환한다`() = runTest {
        // given
        val room = Room(
            Room.Id("room-insert-001"),
            "Inserted Room",
            "inserted-hashed-password"
        )

        // when
        val result = roomRepositoryAdapter.insert(room)

        // then
        assertSame(room, result)
        val entity = roomEntityRepository.findById(room.id.value)
        assertEquals("Inserted Room", entity?.title)
        assertEquals("inserted-hashed-password", entity?.passwordHash)
    }

    @Test
    fun `제목이 존재하면 true를 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.existsByTitle("Seed Room")

        // then
        assertTrue(result)
    }

    @Test
    fun `제목이 존재하지 않으면 false를 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.existsByTitle("Missing Room")

        // then
        assertFalse(result)
    }

    @Test
    fun `닫힌 채팅방의 제목은 존재하지 않는 것으로 조회한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.existsByTitle("Closed Room")

        // then
        assertFalse(result)
    }

    @Test
    fun `닫힌 채팅방의 제목은 새로운 활성 채팅방에서 사용할 수 있다`() = runTest {
        // given
        val room = Room(
            Room.Id("room-reuse-closed-title-001"),
            "Closed Room",
            null
        )

        // when
        val result = roomRepositoryAdapter.insert(room)

        // then
        assertSame(room, result)
        assertTrue(roomRepositoryAdapter.existsByTitle("Closed Room"))
    }

    @Test
    fun `활성 채팅방을 비관적 쓰기 잠금으로 조회하면 방을 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.findActiveByIdForUpdate(Room.Id("room-seed-001"))

        // then
        assertEquals("room-seed-001", result?.id?.value)
        assertEquals("Seed Room", result?.title)
        assertEquals("seed-hashed-password", result?.passwordHash)
    }

    @Test
    fun `닫힌 채팅방을 비관적 쓰기 잠금으로 조회하면 null을 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.findActiveByIdForUpdate(Room.Id("room-closed-001"))

        // then
        assertEquals(null, result)
    }

    @Test
    fun `활성 채팅방을 공유 읽기 잠금으로 조회하면 방을 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.findActiveByIdForRead(Room.Id("room-seed-001"))

        // then
        assertEquals("room-seed-001", result?.id?.value)
        assertEquals("Seed Room", result?.title)
        assertEquals("seed-hashed-password", result?.passwordHash)
    }

    @Test
    fun `닫힌 채팅방을 공유 읽기 잠금으로 조회하면 null을 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.findActiveByIdForRead(Room.Id("room-closed-001"))

        // then
        assertEquals(null, result)
    }

    @Test
    fun `자기 자신을 제외한 제목 중복 여부를 조회하면 중복되지 않은 것으로 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.existsByTitleExceptRoomId(
            "Seed Room",
            Room.Id("room-seed-001")
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `다른 활성 채팅방과 제목이 중복되면 true를 반환한다`() = runTest {
        // when
        val result = roomRepositoryAdapter.existsByTitleExceptRoomId(
            "Seed Room",
            Room.Id("room-other-001")
        )

        // then
        assertTrue(result)
    }

    @Test
    fun `채팅방을 수정하면 제목과 비밀번호를 변경한다`() = runTest {
        // given
        val room = Room(
            Room.Id("room-update-001"),
            "Updated Room",
            "updated-hashed-password"
        )

        // when
        val result = roomRepositoryAdapter.update(room)

        // then
        assertSame(room, result)
        val entity = roomEntityRepository.findById(room.id.value)
        assertEquals("Updated Room", entity?.title)
        assertEquals("updated-hashed-password", entity?.passwordHash)
    }

    @Test
    fun `채팅방을 수정하면서 비밀번호가 null이면 기존 비밀번호를 제거한다`() = runTest {
        // given
        val room = Room(
            Room.Id("room-update-001"),
            "Updated Room Without Password",
            null
        )

        // when
        roomRepositoryAdapter.update(room)

        // then
        val entity = roomEntityRepository.findById(room.id.value)
        assertEquals("Updated Room Without Password", entity?.title)
        assertEquals(null, entity?.passwordHash)
    }

    @Test
    fun `활성 채팅방의 제목은 중복 저장할 수 없다`() = runTest {
        // given
        val room = Room(
            Room.Id("room-duplicate-001"),
            "Seed Room",
            null
        )

        // when & then
        assertFailsWith<DataIntegrityViolationException> {
            roomRepositoryAdapter.insert(room)
        }
    }

}
