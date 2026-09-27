package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.room.service.RoomInfoValidator
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.restapi.context.room.application.exception.DuplicatedTitleException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomInfoUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class UpdateRoomInfoServiceTest {

    @Mock
    lateinit var roomInfoValidator: RoomInfoValidator

    @Mock
    lateinit var passwordHasher: PasswordHasher

    @Mock
    lateinit var roomRepositoryPort: RoomRepositoryPort

    @Mock
    lateinit var participantRepositoryPort: ParticipantRepositoryPort

    @InjectMocks
    lateinit var updateRoomInfoService: UpdateRoomInfoService

    @Test
    fun `호스트가 채팅방 제목과 비밀번호를 변경하면 변경된 방을 저장한다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Old title", null)
        val command = UpdateRoomInfoUseCase.Command(
            "room-001",
            "user-001",
            "New title",
            "NewPassword1"
        )
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(true)
        given(roomRepositoryPort.existsByTitleExceptRoomId(
            argThat<String> { it == "New title" } ?: "New title",
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(false)
        given(passwordHasher.hash("NewPassword1"))
            .willReturn("new-password-hash")

        // when
        updateRoomInfoService.update(command)

        // then
        assertEquals("New title", room.title)
        assertEquals("new-password-hash", room.passwordHash)
        verify(roomInfoValidator).validateTitle("New title")
        verify(roomInfoValidator).validatePassword("NewPassword1")
        verify(passwordHasher).hash("NewPassword1")
        verify(roomRepositoryPort).update(room)
    }

    @Test
    fun `비밀번호가 null이면 기존 비밀번호를 제거한다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Room title", "old-password-hash")
        val command = UpdateRoomInfoUseCase.Command("room-001", "user-001", "Room title", null)
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(true)
        given(roomRepositoryPort.existsByTitleExceptRoomId(
            argThat<String> { it == "Room title" } ?: "Room title",
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(false)

        // when
        updateRoomInfoService.update(command)

        // then
        assertEquals(null, room.passwordHash)
        verify(roomRepositoryPort).update(room)
        verifyNoInteractions(passwordHasher)
    }

    @Test
    fun `존재하지 않는 채팅방이면 호스트 확인과 저장을 하지 않는다`() = runTest {
        // given
        val command = UpdateRoomInfoUseCase.Command("room-001", "user-001", "Room title", null)
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(null)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            updateRoomInfoService.update(command)
        }
        verifyNoInteractions(participantRepositoryPort)
        verify(roomRepositoryPort, only()).findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )
    }

    @Test
    fun `호스트가 아니면 방 정보를 저장하지 않는다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Room title", null)
        val command = UpdateRoomInfoUseCase.Command("room-001", "user-001", "New title", null)
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(false)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            updateRoomInfoService.update(command)
        }
        verifyNoInteractions(roomInfoValidator, passwordHasher)
        verify(roomRepositoryPort).findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )
        verify(participantRepositoryPort, only()).existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )
        verify(roomRepositoryPort, never()).update(room)
    }

    @Test
    fun `제목이 중복되면 방 정보를 저장하지 않는다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Old title", null)
        val command = UpdateRoomInfoUseCase.Command(
            "room-001",
            "user-001",
            "Duplicated title",
            "NewPassword1"
        )
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(true)
        given(roomRepositoryPort.existsByTitleExceptRoomId(
            argThat<String> { it == "Duplicated title" } ?: "Duplicated title",
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(true)

        // when & then
        assertFailsWith<DuplicatedTitleException> {
            updateRoomInfoService.update(command)
        }
        verify(roomRepositoryPort).findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )
        verify(roomRepositoryPort).existsByTitleExceptRoomId(
            argThat<String> { it == "Duplicated title" } ?: "Duplicated title",
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )
        verifyNoInteractions(passwordHasher)
        verify(roomInfoValidator).validateTitle("Duplicated title")
        verify(roomInfoValidator, org.mockito.Mockito.never()).validatePassword("NewPassword1")
        verify(roomRepositoryPort, never()).update(room)
    }

    @Test
    fun `제목이 도메인 규칙을 위반하면 방 정보를 저장하지 않는다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Old title", null)
        val command = UpdateRoomInfoUseCase.Command("room-001", "user-001", "Invalid title", null)
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(true)
        given(roomInfoValidator.validateTitle("Invalid title"))
            .willThrow(DomainRuleViolationException("2-003-005", "Invalid room title."))

        // when & then
        assertFailsWith<DomainRuleViolationException> {
            updateRoomInfoService.update(command)
        }
        verify(roomInfoValidator).validateTitle("Invalid title")
        verify(roomRepositoryPort).findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )
        verifyNoInteractions(passwordHasher)
        verify(roomRepositoryPort, never()).update(room)
    }

    @Test
    fun `비밀번호가 도메인 규칙을 위반하면 해시와 저장을 수행하지 않는다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Room title", null)
        val command = UpdateRoomInfoUseCase.Command("room-001", "user-001", "Room title", "invalid")
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(true)
        given(roomRepositoryPort.existsByTitleExceptRoomId(
            argThat<String> { it == "Room title" } ?: "Room title",
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(false)
        given(roomInfoValidator.validatePassword("invalid"))
            .willThrow(DomainRuleViolationException("2-003-006", "Invalid room password."))

        // when & then
        assertFailsWith<DomainRuleViolationException> {
            updateRoomInfoService.update(command)
        }
        verify(roomInfoValidator).validateTitle("Room title")
        verify(roomInfoValidator).validatePassword("invalid")
        verifyNoInteractions(passwordHasher)
        verify(roomRepositoryPort, never()).update(room)
    }

    @Test
    fun `변경된 값이 없어도 PUT 요청이면 방 정보를 저장한다`() = runTest {
        // given
        val room = Room(Room.Id("room-001"), "Room title", null)
        val command = UpdateRoomInfoUseCase.Command("room-001", "user-001", "Room title", null)
        given(roomRepositoryPort.findActiveByIdForUpdate(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(room)
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001"),
            argThat<User.Id> { it.value == "user-001" } ?: User.Id("user-001")
        )).willReturn(true)
        given(roomRepositoryPort.existsByTitleExceptRoomId(
            argThat<String> { it == "Room title" } ?: "Room title",
            argThat<Room.Id> { it.value == "room-001" } ?: Room.Id("room-001")
        )).willReturn(false)

        // when
        updateRoomInfoService.update(command)

        // then
        verify(roomRepositoryPort).update(room)
        verifyNoInteractions(passwordHasher)
    }

}
