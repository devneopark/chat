package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.RoomParticipantNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.TransferRoomHostUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import com.devneopark.chat.restapi.shared.application.port.outbound.ApplicationLockPort
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

@ExtendWith(MockitoExtension::class)
class TransferRoomHostServiceTest {

    @Mock
    lateinit var roomRepositoryPort: RoomRepositoryPort

    @Mock
    lateinit var participantRepositoryPort: ParticipantRepositoryPort

    @Mock
    lateinit var applicationLockPort: ApplicationLockPort

    @InjectMocks
    lateinit var transferRoomHostService: TransferRoomHostService

    @Test
    fun `호스트는 활성 게스트에게 호스트 역할을 이전한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val requesterUserId = User.Id("user-001")
        val targetUserId = User.Id("user-002")
        val requester = Participant(
            Participant.Id("participant-001"),
            roomId,
            requesterUserId,
            ParticipantRole.HOST,
            Instant.parse("2026-09-15T00:00:00Z")
        )
        val target = Participant(
            Participant.Id("participant-002"),
            roomId,
            targetUserId,
            ParticipantRole.GUEST,
            Instant.parse("2026-09-15T00:00:00Z")
        )
        val command = TransferRoomHostUseCase.Command(
            roomId.value,
            requesterUserId.value,
            targetUserId.value
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdsForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == requesterUserId.value } ?: requesterUserId,
            argThat<User.Id> { it.value == targetUserId.value } ?: targetUserId
        )).willReturn(listOf(requester, target))

        // when
        transferRoomHostService.transfer(command)

        // then
        assertEquals(ParticipantRole.GUEST, requester.role)
        assertEquals(ParticipantRole.HOST, target.role)
        verify(applicationLockPort).lock("room:host:${roomId.value}")
        verify(participantRepositoryPort).updateRole(requester.id, ParticipantRole.GUEST)
        verify(participantRepositoryPort).updateRole(target.id, ParticipantRole.HOST)
    }

    @Test
    fun `존재하지 않는 채팅방이면 잠금과 참여자 조회를 하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val command = TransferRoomHostUseCase.Command(
            roomId.value,
            "user-001",
            "user-002"
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(null)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            transferRoomHostService.transfer(command)
        }
        verifyNoInteractions(applicationLockPort, participantRepositoryPort)
    }

    @Test
    fun `요청자가 호스트가 아니면 참여자 역할을 변경하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val requesterUserId = User.Id("user-001")
        val targetUserId = User.Id("user-002")
        val requester = Participant(
            Participant.Id("participant-001"),
            roomId,
            requesterUserId,
            ParticipantRole.GUEST,
            Instant.parse("2026-09-15T00:00:00Z")
        )
        val target = Participant(
            Participant.Id("participant-002"),
            roomId,
            targetUserId,
            ParticipantRole.GUEST,
            Instant.parse("2026-09-15T00:00:00Z")
        )
        val command = TransferRoomHostUseCase.Command(
            roomId.value,
            requesterUserId.value,
            targetUserId.value
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdsForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == requesterUserId.value } ?: requesterUserId,
            argThat<User.Id> { it.value == targetUserId.value } ?: targetUserId
        )).willReturn(listOf(requester, target))

        // when & then
        assertFailsWith<RoomNotFoundException> {
            transferRoomHostService.transfer(command)
        }
        verify(applicationLockPort).lock("room:host:${roomId.value}")
        verify(participantRepositoryPort, never()).updateRole(requester.id, ParticipantRole.GUEST)
        verify(participantRepositoryPort, never()).updateRole(target.id, ParticipantRole.HOST)
    }

    @Test
    fun `활성 게스트가 아니면 호스트를 변경하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val requesterUserId = User.Id("user-001")
        val targetUserId = User.Id("user-002")
        val requester = Participant(
            Participant.Id("participant-001"),
            roomId,
            requesterUserId,
            ParticipantRole.HOST,
            Instant.parse("2026-09-15T00:00:00Z")
        )
        val command = TransferRoomHostUseCase.Command(
            roomId.value,
            requesterUserId.value,
            targetUserId.value
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdsForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == requesterUserId.value } ?: requesterUserId,
            argThat<User.Id> { it.value == targetUserId.value } ?: targetUserId
        )).willReturn(listOf(requester))

        // when & then
        assertFailsWith<RoomParticipantNotFoundException> {
            transferRoomHostService.transfer(command)
        }
        verify(participantRepositoryPort, never()).updateRole(requester.id, ParticipantRole.GUEST)
    }

    @Test
    fun `대상이 호스트 자신이면 호스트를 변경하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val requesterUserId = User.Id("user-001")
        val requester = Participant(
            Participant.Id("participant-001"),
            roomId,
            requesterUserId,
            ParticipantRole.HOST,
            Instant.parse("2026-09-15T00:00:00Z")
        )
        val command = TransferRoomHostUseCase.Command(
            roomId.value,
            requesterUserId.value,
            requesterUserId.value
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.findActiveByRoomIdAndUserIdsForUpdate(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == requesterUserId.value } ?: requesterUserId,
            argThat<User.Id> { it.value == requesterUserId.value } ?: requesterUserId
        )).willReturn(listOf(requester))

        // when & then
        assertFailsWith<RoomParticipantNotFoundException> {
            transferRoomHostService.transfer(command)
        }
        verify(participantRepositoryPort, never()).updateRole(requester.id, ParticipantRole.GUEST)
    }

}
