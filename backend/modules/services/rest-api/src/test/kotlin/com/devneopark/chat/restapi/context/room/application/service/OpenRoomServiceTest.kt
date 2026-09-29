package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition as RoomExceptionDefinition
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.restapi.context.room.application.policy.RoomInfoPolicy
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.room.application.exception.DuplicatedTitleException
import com.devneopark.chat.restapi.context.room.application.exception.InvalidCapacityException
import com.devneopark.chat.restapi.context.room.application.policy.AdmissionSlotPolicy
import com.devneopark.chat.restapi.context.room.application.port.inbound.OpenRoomUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant

@ExtendWith(MockitoExtension::class)
class OpenRoomServiceTest {

    @Mock
    lateinit var roomInfoPolicy: RoomInfoPolicy

    @Mock
    lateinit var passwordHasher: PasswordHasher

    @Mock
    lateinit var admissionSlotPolicy: AdmissionSlotPolicy

    @Mock
    lateinit var roomRepositoryPort: RoomRepositoryPort

    @Mock
    lateinit var idGenerator: IdGenerator

    @Mock
    lateinit var clock: Clock

    @Mock
    lateinit var participantRepositoryPort: ParticipantRepositoryPort

    @Mock
    lateinit var admissionSlotRepositoryPort: AdmissionSlotRepositoryPort

    @InjectMocks
    lateinit var openRoomService: OpenRoomService

    @Test
    fun `비밀번호가 있는 방을 개설하고 호스트 참여자와 입장 슬롯을 생성한다`() = runTest {
        // given
        val title = "Room title"
        val rawPassword = "RawP@ssword1"
        val passwordHash = "hashed-password"
        val capacity = 10
        val hostUserId = "user-001"
        val roomIdValue = "room-001"
        val participantIdValue = "participant-001"
        val joinedAt = Instant.parse("2026-09-14T00:00:00Z")
        val command = OpenRoomUseCase.Command(title, rawPassword, capacity, hostUserId)

        given(roomRepositoryPort.existsByTitle(title))
            .willReturn(false)
        given(passwordHasher.hash(rawPassword))
            .willReturn(passwordHash)
        given(idGenerator.generate())
            .willReturn(roomIdValue, participantIdValue)
        given(clock.instant())
            .willReturn(joinedAt)

        // when
        val result = openRoomService.open(command)

        // then
        assertEquals(roomIdValue, result.roomId)
        verify(roomInfoPolicy).validateTitle(title)
        verify(roomInfoPolicy).validatePassword(rawPassword)
        verify(admissionSlotPolicy).validateCapacity(capacity)
        verify(passwordHasher).hash(rawPassword)
        verify(roomRepositoryPort).existsByTitle(title)
        verify(roomRepositoryPort).insert(
            argThat<Room> {
                it.id.value == roomIdValue &&
                    it.title == title &&
                    passwordHash == it.passwordHash
            } ?: Room(Room.Id(roomIdValue), title, passwordHash)
        )
        verify(participantRepositoryPort).insert(
            argThat<Participant> {
                it.id.value == participantIdValue &&
                    it.roomId.value == roomIdValue &&
                    it.userId.value == hostUserId &&
                    it.role == ParticipantRole.HOST &&
                    joinedAt.toKotlinInstant() == it.joinedAt
            } ?: Participant(
                Participant.Id(participantIdValue),
                Room.Id(roomIdValue),
                User.Id(hostUserId),
                ParticipantRole.HOST,
                joinedAt.toKotlinInstant()
            )
        )
        verify(admissionSlotRepositoryPort).provision(
            argThat<RoomId> {
                it.value == roomIdValue
            } ?: Room.Id(roomIdValue),
            eq(capacity),
            argThat<ParticipantId> {
                it.value == participantIdValue
            } ?: Participant.Id(participantIdValue)
        )
    }

    @Test
    fun `비밀번호가 없는 방을 개설하면 비밀번호 해시를 생성하지 않는다`() = runTest {
        // given
        val title = "Open room"
        val capacity = 5
        val command = OpenRoomUseCase.Command(title, null, capacity, "user-001")
        given(roomRepositoryPort.existsByTitle(title))
            .willReturn(false)
        given(idGenerator.generate())
            .willReturn("room-001", "participant-001")
        given(clock.instant())
            .willReturn(Instant.parse("2026-09-14T00:00:00Z"))

        // when
        val result = openRoomService.open(command)

        // then
        assertEquals("room-001", result.roomId)
        verify(roomInfoPolicy).validateTitle(title)
        verify(roomInfoPolicy, never()).validatePassword(anyString())
        verify(passwordHasher, never()).hash(anyString())
        verify(roomRepositoryPort).insert(
            argThat<Room> {
                it.id.value == "room-001" &&
                    it.title == title &&
                    it.passwordHash == null
            } ?: Room(Room.Id("room-001"), title, null)
        )
    }

    @Test
    fun `중복된 제목이면 DuplicatedTitleException을 던지고 생성하지 않는다`() = runTest {
        // given
        val title = "Duplicated room"
        val command = OpenRoomUseCase.Command(title, null, 10, "user-001")
        given(roomRepositoryPort.existsByTitle(title))
            .willReturn(true)

        // when
        val exception = assertFailsWith<DuplicatedTitleException> {
            openRoomService.open(command)
        }

        // then
        assertEquals("2-003-001", exception.code)
        assertEquals("Title duplicated.", exception.message)
        verify(roomRepositoryPort, only())
            .existsByTitle(title)
        verifyNoInteractions(
            passwordHasher,
            idGenerator,
            clock,
            participantRepositoryPort,
            admissionSlotRepositoryPort
        )
    }

    @Test
    fun `허용 범위를 벗어난 정원이면 예외를 던지고 생성하지 않는다`() = runTest {
        // given
        val capacity = 1
        val command = OpenRoomUseCase.Command("Room", null, capacity, "user-001")
        given(admissionSlotPolicy.validateCapacity(capacity))
            .willThrow(InvalidCapacityException())

        // when
        val exception = assertFailsWith<InvalidCapacityException> {
            openRoomService.open(command)
        }

        // then
        assertEquals("2-003-002", exception.code)
        assertEquals("Capacity out of bound.", exception.message)
        verify(admissionSlotPolicy).validateCapacity(capacity)
        verifyNoInteractions(
            roomRepositoryPort,
            passwordHasher,
            idGenerator,
            clock,
            participantRepositoryPort,
            admissionSlotRepositoryPort
        )
    }

    @Test
    fun `제목이 허용된 형식이 아니면 생성하지 않는다`() = runTest {
        // given
        val title = "Invalid title"
        val command = OpenRoomUseCase.Command(title, null, 10, "user-001")
        given(roomInfoPolicy.validateTitle(title))
            .willThrow(
                DomainRuleViolationException(
                    RoomExceptionDefinition.INVALID_ROOM_TITLE.code,
                    RoomExceptionDefinition.INVALID_ROOM_TITLE.message
                )
            )

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            openRoomService.open(command)
        }

        // then
        assertEquals(RoomExceptionDefinition.INVALID_ROOM_TITLE.code, exception.code)
        assertEquals(RoomExceptionDefinition.INVALID_ROOM_TITLE.message, exception.message)
        verify(roomInfoPolicy).validateTitle(title)
        verifyNoInteractions(
            admissionSlotPolicy,
            roomRepositoryPort,
            passwordHasher,
            idGenerator,
            clock,
            participantRepositoryPort,
            admissionSlotRepositoryPort
        )
    }

    @Test
    fun `비밀번호가 허용된 형식이 아니면 생성하지 않는다`() = runTest {
        // given
        val title = "Private room"
        val rawPassword = "invalid"
        val command = OpenRoomUseCase.Command(title, rawPassword, 10, "user-001")
        given(roomInfoPolicy.validatePassword(rawPassword))
            .willThrow(
                DomainRuleViolationException(
                    RoomExceptionDefinition.INVALID_ROOM_PASSWORD.code,
                    RoomExceptionDefinition.INVALID_ROOM_PASSWORD.message
                )
            )

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            openRoomService.open(command)
        }

        // then
        assertEquals(RoomExceptionDefinition.INVALID_ROOM_PASSWORD.code, exception.code)
        assertEquals(RoomExceptionDefinition.INVALID_ROOM_PASSWORD.message, exception.message)
        verify(roomInfoPolicy).validateTitle(title)
        verify(roomInfoPolicy).validatePassword(rawPassword)
        verifyNoInteractions(
            admissionSlotPolicy,
            roomRepositoryPort,
            passwordHasher,
            idGenerator,
            clock,
            participantRepositoryPort,
            admissionSlotRepositoryPort
        )
    }

}
