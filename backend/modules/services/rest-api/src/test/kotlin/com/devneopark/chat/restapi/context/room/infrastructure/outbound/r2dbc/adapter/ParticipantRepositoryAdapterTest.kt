package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.repository.ParticipantEntityRepository
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
import kotlin.time.Instant

@DataR2dbcTest
@ActiveProfiles("test")
@Import(ParticipantRepositoryAdapter::class)
class ParticipantRepositoryAdapterTest {

    @Autowired
    lateinit var participantRepositoryAdapter: ParticipantRepositoryAdapter

    @Autowired
    lateinit var participantEntityRepository: ParticipantEntityRepository

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
                "classpath:init/ParticipantRepositoryAdapterTest.sql"
            }
        }

    }

    @Test
    fun `insert하면 실제 저장되고 입력한 participant를 반환한다`() = runTest {
        // given
        val joinedAt = Instant.parse("2026-09-15T00:00:00Z")
        val participant = Participant(
            Participant.Id("participant-insert-001"),
            Room.Id("participant-room-001"),
            User.Id("participant-user-002"),
            ParticipantRole.HOST,
            joinedAt
        )

        // when
        val result = participantRepositoryAdapter.insert(participant)

        // then
        assertSame(participant, result)
        val entity = participantEntityRepository.findById(participant.id.value)
        assertEquals("participant-room-001", entity?.roomId)
        assertEquals("participant-user-002", entity?.userId)
        assertEquals("HOST", entity?.role)
        assertEquals(joinedAt.toString(), entity?.joinedAt?.toString())
    }

    @Test
    fun `활성 상태의 같은 room과 user 조합은 중복 저장할 수 없다`() = runTest {
        // given
        val participant = Participant(
            Participant.Id("participant-duplicate-001"),
            Room.Id("participant-room-001"),
            User.Id("participant-user-001"),
            ParticipantRole.GUEST,
            Instant.parse("2026-09-15T01:00:00Z")
        )

        // when & then
        assertFailsWith<DataIntegrityViolationException> {
            participantRepositoryAdapter.insert(participant)
        }
    }

    @Test
    fun `탈퇴한 참여자와 같은 room과 user 조합은 다시 저장할 수 있다`() = runTest {
        // given
        val joinedAt = Instant.parse("2026-09-15T02:00:00Z")
        val participant = Participant(
            Participant.Id("participant-rejoin-001"),
            Room.Id("participant-room-001"),
            User.Id("participant-user-003"),
            ParticipantRole.GUEST,
            joinedAt
        )

        // when
        val result = participantRepositoryAdapter.insert(participant)

        // then
        assertSame(participant, result)
        val entity = participantEntityRepository.findById(participant.id.value)
        assertEquals("participant-room-001", entity?.roomId)
        assertEquals("participant-user-003", entity?.userId)
        assertEquals("GUEST", entity?.role)
    }

    @Test
    fun `활성 호스트를 조회하면 true를 반환한다`() = runTest {
        // when
        val result = participantRepositoryAdapter.existsActiveHostForRead(
            Room.Id("participant-room-001"),
            User.Id("participant-user-001")
        )

        // then
        assertTrue(result)
    }

    @Test
    fun `활성 게스트를 호스트로 조회하면 false를 반환한다`() = runTest {
        // when
        val result = participantRepositoryAdapter.existsActiveHostForRead(
            Room.Id("participant-room-001"),
            User.Id("participant-user-004")
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `탈퇴한 호스트를 조회하면 false를 반환한다`() = runTest {
        // when
        val result = participantRepositoryAdapter.existsActiveHostForRead(
            Room.Id("participant-room-001"),
            User.Id("participant-user-005")
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `존재하지 않는 호스트를 조회하면 false를 반환한다`() = runTest {
        // when
        val result = participantRepositoryAdapter.existsActiveHostForRead(
            Room.Id("participant-room-001"),
            User.Id("participant-missing-001")
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `존재하지 않는 room을 참조하는 참여자는 저장할 수 없다`() = runTest {
        // given
        val participant = Participant(
            Participant.Id("participant-missing-room-001"),
            Room.Id("missing-room-001"),
            User.Id("participant-user-002"),
            ParticipantRole.GUEST,
            Instant.parse("2026-09-15T03:00:00Z")
        )

        // when & then
        assertFailsWith<DataIntegrityViolationException> {
            participantRepositoryAdapter.insert(participant)
        }
    }

}
