package com.devneopark.chat.restapi.context.room.application.service

import com.devneopark.chat.lib.domain.admission_slot.model.AdmissionSlot
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.room.application.exception.CapacityReductionUnavailableException
import com.devneopark.chat.restapi.context.room.application.exception.InvalidCapacityException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.policy.AdmissionSlotPolicy
import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomCapacityUseCase
import com.devneopark.chat.restapi.context.room.application.port.outbound.AdmissionSlotRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.ParticipantRepositoryPort
import com.devneopark.chat.restapi.context.room.application.port.outbound.RoomRepositoryPort
import com.devneopark.chat.restapi.shared.application.port.outbound.ApplicationLockPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class UpdateRoomCapacityServiceTest {

    @Mock
    lateinit var admissionSlotPolicy: AdmissionSlotPolicy

    @Mock
    lateinit var roomRepositoryPort: RoomRepositoryPort

    @Mock
    lateinit var participantRepositoryPort: ParticipantRepositoryPort

    @Mock
    lateinit var admissionSlotRepositoryPort: AdmissionSlotRepositoryPort

    @Mock
    lateinit var applicationLockPort: ApplicationLockPort

    @InjectMocks
    lateinit var updateRoomCapacityService: UpdateRoomCapacityService

    @Test
    fun `정원을 늘리면 증가한 수만큼 빈 슬롯을 추가한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 5)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        ))
            .willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        ))
            .willReturn(true)
        given(admissionSlotRepositoryPort.countByRoomId(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(3)
        given(admissionSlotRepositoryPort.findMaxSlotNumberByRoomId(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(3)

        // when
        updateRoomCapacityService.update(command)

        // then
        verify(admissionSlotPolicy).validateCapacity(5)
        verify(applicationLockPort).lock("room:capacity:${roomId.value}")
        verify(admissionSlotRepositoryPort).findMaxSlotNumberByRoomId(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )
        verify(admissionSlotRepositoryPort).provisionAdditionalSlots(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(3),
            eq(2)
        )
        verify(admissionSlotRepositoryPort, never()).findEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(2)
        )
        verify(admissionSlotRepositoryPort, never()).deleteAllByIds(org.mockito.ArgumentMatchers.anyList())
    }

    @Test
    fun `정원을 줄이면 필요한 수만큼 빈 슬롯을 삭제한다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 3)
        val removableSlots = listOf(
            AdmissionSlot.Id("room-001", 5),
            AdmissionSlot.Id("room-001", 4)
        )
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        ))
            .willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        ))
            .willReturn(true)
        given(admissionSlotRepositoryPort.countByRoomId(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(5)
        given(admissionSlotRepositoryPort.findEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(2)
        ))
            .willReturn(removableSlots)

        // when
        updateRoomCapacityService.update(command)

        // then
        verify(applicationLockPort).lock("room:capacity:${roomId.value}")
        verify(admissionSlotRepositoryPort)
            .findEmptyByRoomIdForUpdateSkipLocked(
                argThat<Room.Id> { it.value == roomId.value } ?: roomId,
                eq(2)
            )
        verify(admissionSlotRepositoryPort).deleteAllByIds(removableSlots)
        verify(admissionSlotRepositoryPort, never()).provisionAdditionalSlots(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(0),
            eq(2)
        )
    }

    @Test
    fun `정원 감소에 필요한 빈 슬롯이 부족하면 예외를 던지고 삭제하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 2)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        ))
            .willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        ))
            .willReturn(true)
        given(admissionSlotRepositoryPort.countByRoomId(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(5)
        given(admissionSlotRepositoryPort.findEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(3)
        ))
            .willReturn(listOf(AdmissionSlot.Id("room-001", 5)))

        // when & then
        assertFailsWith<CapacityReductionUnavailableException> {
            updateRoomCapacityService.update(command)
        }
        verify(admissionSlotRepositoryPort)
            .findEmptyByRoomIdForUpdateSkipLocked(
                argThat<Room.Id> { it.value == roomId.value } ?: roomId,
                eq(3)
            )
        verify(admissionSlotRepositoryPort, never()).deleteAllByIds(org.mockito.ArgumentMatchers.anyList())
    }

    @Test
    fun `현재 정원과 같은 정원으로 변경하면 슬롯을 변경하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 5)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        ))
            .willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        ))
            .willReturn(true)
        given(admissionSlotRepositoryPort.countByRoomId(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(5)

        // when
        updateRoomCapacityService.update(command)

        // then
        verify(applicationLockPort).lock("room:capacity:${roomId.value}")
        verify(admissionSlotRepositoryPort, never()).provisionAdditionalSlots(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(0),
            eq(1)
        )
        verify(admissionSlotRepositoryPort, never()).findEmptyByRoomIdForUpdateSkipLocked(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            eq(1)
        )
        verify(admissionSlotPolicy).validateCapacity(5)
    }

    @Test
    fun `존재하지 않는 채팅방이면 호스트 확인과 슬롯 변경을 하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 5)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        )).willReturn(null)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            updateRoomCapacityService.update(command)
        }
        verifyNoInteractions(
            participantRepositoryPort,
            admissionSlotPolicy,
            admissionSlotRepositoryPort,
            applicationLockPort
        )
    }

    @Test
    fun `호스트가 아니면 슬롯을 변경하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 5)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        ))
            .willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        ))
            .willReturn(false)

        // when & then
        assertFailsWith<RoomNotFoundException> {
            updateRoomCapacityService.update(command)
        }
        verifyNoInteractions(admissionSlotPolicy, admissionSlotRepositoryPort, applicationLockPort)
    }

    @Test
    fun `허용되지 않은 정원이면 슬롯을 변경하지 않는다`() = runTest {
        // given
        val roomId = Room.Id("room-001")
        val userId = User.Id("user-001")
        val command = UpdateRoomCapacityUseCase.Command("room-001", "user-001", 1)
        given(roomRepositoryPort.findActiveByIdForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId
        ))
            .willReturn(Room(roomId, "Room", null))
        given(participantRepositoryPort.existsActiveHostForRead(
            argThat<Room.Id> { it.value == roomId.value } ?: roomId,
            argThat<User.Id> { it.value == userId.value } ?: userId
        ))
            .willReturn(true)
        given(admissionSlotPolicy.validateCapacity(1))
            .willThrow(InvalidCapacityException())

        // when & then
        assertFailsWith<InvalidCapacityException> {
            updateRoomCapacityService.update(command)
        }
        verifyNoInteractions(admissionSlotRepositoryPort, applicationLockPort)
    }

}
