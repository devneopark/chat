package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.admission_slot.model.AdmissionSlot
import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.room.application.exception.AlreadyJoinedRoomException
import com.devneopark.chat.restapi.context.room.application.exception.RoomFullException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.WrongRoomPasswordException
import com.devneopark.chat.restapi.context.room.application.port.inbound.JoinRoomUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant

@ExtendWith(MockitoExtension::class)
class JoinRoomServiceTest {

    @Mock
    lateinit var roomRepositoryPort: RoomRepositoryPort

    @Mock
    lateinit var participantRepositoryPort: ParticipantRepositoryPort

    @Mock
    lateinit var admissionSlotRepositoryPort: AdmissionSlotRepositoryPort

    @Mock
    lateinit var passwordHasher: PasswordHasher

    @Mock
    lateinit var idGenerator: IdGenerator

    @Mock
    lateinit var clock: Clock

    @InjectMocks
    lateinit var joinRoomService: JoinRoomService

    @Test
    fun `공개 채팅방에 입장하면 게스트 참여자와 슬롯 점유를 생성한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val slotId = AdmissionSlot.Id(roomId.value, 1)
        val joinedAt = Instant.parse("2026-09-26T00:00:00Z")
        val command = JoinRoomUseCase.Command(roomId.value, userId.value, null)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)
        given(admissionSlotRepositoryPort.findFirstEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(slotId)
        given(idGenerator.generate()).willReturn("participant-001")
        given(clock.instant()).willReturn(joinedAt)

        // when
        joinRoomService.join(command)

        // then
        verify(participantRepositoryPort).insert(
            argThat<Participant> {
                it.id.value == "participant-001" &&
                    it.roomId.value == roomId.value &&
                    it.userId.value == userId.value &&
                    it.role == ParticipantRole.GUEST &&
                    it.joinedAt == joinedAt.toKotlinInstant()
            } ?: Participant(
                Participant.Id("participant-001"),
                roomId,
                userId,
                ParticipantRole.GUEST,
                joinedAt.toKotlinInstant()
            )
        )
        verify(admissionSlotRepositoryPort).assignParticipant(
            argThat<AdmissionSlot.Id> {
                it.roomId == roomId.value && it.number == 1
            } ?: slotId,
            argThat<ParticipantId> { it.value == "participant-001" } ?: Participant.Id("participant-001")
        )
        verify(passwordHasher, never()).matches(anyString(), anyString())
    }

    @Test
    fun `비밀번호가 있는 채팅방에 올바른 비밀번호로 입장한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = JoinRoomUseCase.Command(roomId.value, userId.value, "RoomPassword1")
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", "hashed-password"))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)
        given(passwordHasher.matches("RoomPassword1", "hashed-password"))
            .willReturn(true)
        given(admissionSlotRepositoryPort.findFirstEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(AdmissionSlot.Id(roomId.value, 1))
        given(idGenerator.generate()).willReturn("participant-001")
        given(clock.instant()).willReturn(Instant.parse("2026-09-26T00:00:00Z"))

        // when
        joinRoomService.join(command)

        // then
        verify(passwordHasher).matches("RoomPassword1", "hashed-password")
        verify(participantRepositoryPort).insert(
            argThat<Participant> {
                it.roomId.value == roomId.value &&
                    it.userId.value == userId.value &&
                    it.role == ParticipantRole.GUEST
            } ?: Participant(
                Participant.Id("participant-001"),
                roomId,
                userId,
                ParticipantRole.GUEST,
                Instant.parse("2026-09-26T00:00:00Z").toKotlinInstant()
            )
        )
        verify(admissionSlotRepositoryPort).assignParticipant(
            argThat<AdmissionSlot.Id> {
                it.roomId == roomId.value && it.number == 1
            } ?: AdmissionSlot.Id(roomId.value, 1),
            argThat<ParticipantId> { it.value == "participant-001" } ?: Participant.Id("participant-001")
        )
    }

    @Test
    fun `이미 참여 중인 사용자가 다시 입장하면 이미 참여 중인 방 예외를 던진다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val existingParticipant = Participant(
            Participant.Id("participant-001"),
            roomId,
            userId,
            ParticipantRole.GUEST,
            Instant.parse("2026-09-25T00:00:00Z").toKotlinInstant()
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(existingParticipant)

        // when
        val exception = assertFailsWith<AlreadyJoinedRoomException> {
            joinRoomService.join(JoinRoomUseCase.Command(roomId.value, userId.value, null))
        }

        // then
        assertEquals("2-003-009", exception.code)
        assertEquals("Already joined room.", exception.message)
        verifyNoInteractions(admissionSlotRepositoryPort, passwordHasher, idGenerator, clock)
        verify(participantRepositoryPort, never()).insert(
            argThat<Participant> { true } ?: existingParticipant
        )
    }

    @Test
    fun `보호된 채팅방에 비밀번호가 없으면 비밀번호 예외를 던지고 입장하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", "hashed-password"))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)

        // when & then
        val exception = assertFailsWith<WrongRoomPasswordException> {
            joinRoomService.join(JoinRoomUseCase.Command(roomId.value, userId.value, null))
        }
        assertEquals("2-003-007", exception.code)
        assertEquals("Wrong room password.", exception.message)
        verifyNoInteractions(passwordHasher, admissionSlotRepositoryPort, idGenerator, clock)
        verify(participantRepositoryPort, never()).insert(
            argThat<Participant> { true } ?: Participant(
                Participant.Id("participant-001"),
                roomId,
                userId,
                ParticipantRole.GUEST,
                Instant.parse("2026-09-26T00:00:00Z").toKotlinInstant()
            )
        )
    }

    @Test
    fun `채팅방이 없으면 입장하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(null)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            joinRoomService.join(JoinRoomUseCase.Command(roomId.value, "user-001", null))
        }
        verifyNoInteractions(
            participantRepositoryPort,
            admissionSlotRepositoryPort,
            passwordHasher,
            idGenerator,
            clock
        )
    }

    @Test
    fun `비밀번호가 틀리면 슬롯을 조회하거나 참여자를 생성하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", "hashed-password"))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)
        given(passwordHasher.matches("WrongPassword1", "hashed-password"))
            .willReturn(false)

        // when & then
        val exception = assertFailsWith<WrongRoomPasswordException> {
            joinRoomService.join(JoinRoomUseCase.Command(roomId.value, userId.value, "WrongPassword1"))
        }
        assertEquals("2-003-007", exception.code)
        assertEquals("Wrong room password.", exception.message)
        verifyNoInteractions(admissionSlotRepositoryPort, idGenerator, clock)
        verify(participantRepositoryPort, never()).insert(
            argThat<Participant> { true } ?: Participant(
                Participant.Id("participant-001"),
                roomId,
                userId,
                ParticipantRole.GUEST,
                Instant.parse("2026-09-26T00:00:00Z").toKotlinInstant()
            )
        )
    }

    @Test
    fun `빈 슬롯이 없으면 참여자를 생성하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)
        given(admissionSlotRepositoryPort.findFirstEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(null)

        // when & then
        val exception = assertFailsWith<RoomFullException> {
            joinRoomService.join(JoinRoomUseCase.Command(roomId.value, userId.value, null))
        }
        assertEquals("2-003-008", exception.code)
        assertEquals("Room is full.", exception.message)
        verifyNoInteractions(idGenerator, clock)
        verify(participantRepositoryPort, never()).insert(
            argThat<Participant> { true } ?: Participant(
                Participant.Id("participant-001"),
                roomId,
                userId,
                ParticipantRole.GUEST,
                Instant.parse("2026-09-26T00:00:00Z").toKotlinInstant()
            )
        )
    }

    @Test
    fun `참여자 저장에 실패하면 슬롯을 점유하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val slotId = AdmissionSlot.Id(roomId.value, 1)
        val joinedAt = Instant.parse("2026-09-26T00:00:00Z")
        val participant = Participant(
            Participant.Id("participant-001"),
            roomId,
            userId,
            ParticipantRole.GUEST,
            joinedAt.toKotlinInstant()
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)
        given(admissionSlotRepositoryPort.findFirstEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(slotId)
        given(idGenerator.generate()).willReturn(participant.id.value)
        given(clock.instant()).willReturn(joinedAt)
        given(participantRepositoryPort.insert(
            argThat<Participant> {
                it.id.value == participant.id.value &&
                    it.roomId.value == participant.roomId.value &&
                    it.userId.value == participant.userId.value &&
                    it.role == participant.role &&
                    it.joinedAt == participant.joinedAt
            } ?: participant
        )).willThrow(IllegalStateException())

        // when & then
        assertFailsWith<IllegalStateException> {
            joinRoomService.join(JoinRoomUseCase.Command(roomId.value, userId.value, null))
        }
        verify(admissionSlotRepositoryPort, never()).assignParticipant(
            argThat<AdmissionSlot.Id> {
                it.roomId == roomId.value && it.number == 1
            } ?: slotId,
            argThat<ParticipantId> { it.value == participant.id.value } ?: participant.id
        )
    }

}
