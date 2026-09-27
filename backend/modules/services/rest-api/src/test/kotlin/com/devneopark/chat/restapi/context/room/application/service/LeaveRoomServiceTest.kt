package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.RoomParticipantNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.LeaveRoomUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant

@ExtendWith(MockitoExtension::class)
class LeaveRoomServiceTest {

    @Mock
    lateinit var roomRepositoryPort: RoomRepositoryPort

    @Mock
    lateinit var participantRepositoryPort: ParticipantRepositoryPort

    @Mock
    lateinit var admissionSlotRepositoryPort: AdmissionSlotRepositoryPort

    @Mock
    lateinit var clock: Clock

    @InjectMocks
    lateinit var leaveRoomService: LeaveRoomService

    @Test
    fun `게스트가 퇴장하면 참여자와 슬롯을 정리한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val participant = Participant(
            Participant.Id("participant-001"),
            roomId,
            userId,
            ParticipantRole.GUEST,
            Instant.parse("2026-09-25T00:00:00Z").toKotlinInstant()
        )
        val exitedAt = Instant.parse("2026-09-26T00:00:00Z")
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(participant)
        given(clock.instant()).willReturn(exitedAt)

        // when
        leaveRoomService.leave(LeaveRoomUseCase.Command(roomId.value, userId.value))

        // then
        verify(participantRepositoryPort).markExited(participant.id, exitedAt.toKotlinInstant())
        verify(admissionSlotRepositoryPort).releaseParticipant(participant.id)
        verify(participantRepositoryPort, never()).findOldestActiveGuestForUpdate(roomId)
        verify(participantRepositoryPort, never()).updateRole(participant.id, ParticipantRole.HOST)
        verify(roomRepositoryPort, never()).close(roomId, exitedAt.toKotlinInstant())
    }

    @Test
    fun `호스트가 퇴장하면 가장 오래된 게스트에게 호스트 역할을 넘긴다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val hostUserId = User.Id("user-001")
        val host = Participant(
            Participant.Id("participant-001"),
            roomId,
            hostUserId,
            ParticipantRole.HOST,
            Instant.parse("2026-09-25T00:00:00Z").toKotlinInstant()
        )
        val hostCandidate = Participant(
            Participant.Id("participant-002"),
            roomId,
            User.Id("user-002"),
            ParticipantRole.GUEST,
            Instant.parse("2026-09-25T01:00:00Z").toKotlinInstant()
        )
        val exitedAt = Instant.parse("2026-09-26T00:00:00Z")
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == hostUserId.value } ?: hostUserId
        )).willReturn(host)
        given(participantRepositoryPort.findOldestActiveGuestForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(hostCandidate)
        given(clock.instant()).willReturn(exitedAt)

        // when
        leaveRoomService.leave(LeaveRoomUseCase.Command(roomId.value, hostUserId.value))

        // then
        verify(participantRepositoryPort).markExited(host.id, exitedAt.toKotlinInstant())
        verify(admissionSlotRepositoryPort).releaseParticipant(host.id)
        verify(participantRepositoryPort).updateRole(hostCandidate.id, ParticipantRole.HOST)
        verify(roomRepositoryPort, never()).close(roomId, exitedAt.toKotlinInstant())
    }

    @Test
    fun `유일한 호스트가 퇴장하면 방을 폐쇄한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val hostUserId = User.Id("user-001")
        val host = Participant(
            Participant.Id("participant-001"),
            roomId,
            hostUserId,
            ParticipantRole.HOST,
            Instant.parse("2026-09-25T00:00:00Z").toKotlinInstant()
        )
        val exitedAt = Instant.parse("2026-09-26T00:00:00Z")
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == hostUserId.value } ?: hostUserId
        )).willReturn(host)
        given(participantRepositoryPort.findOldestActiveGuestForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(null)
        given(clock.instant()).willReturn(exitedAt)

        // when
        leaveRoomService.leave(LeaveRoomUseCase.Command(roomId.value, hostUserId.value))

        // then
        verify(participantRepositoryPort).markExited(host.id, exitedAt.toKotlinInstant())
        verify(admissionSlotRepositoryPort).releaseParticipant(host.id)
        verify(roomRepositoryPort).close(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<kotlin.time.Instant> { it == exitedAt.toKotlinInstant() }
                ?: exitedAt.toKotlinInstant()
        )
        verify(participantRepositoryPort, never()).updateRole(host.id, ParticipantRole.HOST)
    }

    @Test
    fun `존재하지 않는 채팅방이면 참여자를 조회하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(null)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            leaveRoomService.leave(LeaveRoomUseCase.Command(roomId.value, "user-001"))
        }
        verifyNoInteractions(participantRepositoryPort, admissionSlotRepositoryPort, clock)
    }

    @Test
    fun `활성 참여자가 아니면 퇴장 처리를 하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        )).willReturn(null)

        // when & then
        assertFailsWith<RoomParticipantNotFoundException> {
            leaveRoomService.leave(LeaveRoomUseCase.Command(roomId.value, userId.value))
        }
        verifyNoInteractions(admissionSlotRepositoryPort, clock)
        verify(participantRepositoryPort, never()).findOldestActiveGuestForUpdate(roomId)
    }

}
