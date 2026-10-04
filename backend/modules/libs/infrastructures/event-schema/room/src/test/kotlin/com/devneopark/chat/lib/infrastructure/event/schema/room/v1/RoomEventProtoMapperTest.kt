package com.devneopark.chat.lib.infrastructure.event.schema.room.v1

import com.devneopark.chat.lib.domain.room.event.v1.HostLeft
import com.devneopark.chat.lib.domain.room.event.v1.HostTransferred
import com.devneopark.chat.lib.domain.room.event.v1.ParticipantJoined
import com.devneopark.chat.lib.domain.room.event.v1.ParticipantLeft
import com.devneopark.chat.lib.domain.room.event.v1.RoomCapacityChanged
import com.devneopark.chat.lib.domain.room.event.v1.RoomClosed
import com.devneopark.chat.lib.domain.room.event.v1.RoomInfoChanged
import com.devneopark.chat.lib.domain.room.event.v1.RoomOpened
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.HostLeft as HostLeftPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.HostTransferred as HostTransferredPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.ParticipantJoined as ParticipantJoinedPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.ParticipantLeft as ParticipantLeftPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomCapacityChanged as RoomCapacityChangedPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomClosed as RoomClosedPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomInfoChanged as RoomInfoChangedPayload
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomOpened as RoomOpenedPayload
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomEventProtoMapperTest {

    @Test
    fun `채팅방 생성 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = RoomOpened(
            roomId = "room-id",
            hostParticipantId = "participant-id",
            hostUserId = "user-id",
            openedAt = eventTime,
        )

        val payload: RoomOpenedPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.hostParticipantId, payload.hostParticipantId)
        assertEquals(event.hostUserId, payload.hostUserId)
        assertEquals(eventTime.epochSecond, payload.openedAt.seconds)
        assertEquals(eventTime.nano, payload.openedAt.nanos)
    }

    @Test
    fun `채팅방 폐쇄 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = RoomClosed(
            roomId = "room-id",
            hostParticipantId = "participant-id",
            hostUserId = "user-id",
            closedAt = eventTime,
        )

        val payload: RoomClosedPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.hostParticipantId, payload.hostParticipantId)
        assertEquals(event.hostUserId, payload.hostUserId)
        assertEquals(eventTime.epochSecond, payload.closedAt.seconds)
        assertEquals(eventTime.nano, payload.closedAt.nanos)
    }

    @Test
    fun `참여자 입장 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = ParticipantJoined(
            roomId = "room-id",
            participantId = "participant-id",
            userId = "user-id",
            role = "GUEST",
            joinedAt = eventTime,
        )

        val payload: ParticipantJoinedPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.participantId, payload.participantId)
        assertEquals(event.userId, payload.userId)
        assertEquals(event.role, payload.role)
        assertEquals(eventTime.epochSecond, payload.joinedAt.seconds)
        assertEquals(eventTime.nano, payload.joinedAt.nanos)
    }

    @Test
    fun `참여자 퇴장 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = ParticipantLeft(
            roomId = "room-id",
            participantId = "participant-id",
            userId = "user-id",
            roleBeforeLeave = "GUEST",
            leftAt = eventTime,
        )

        val payload: ParticipantLeftPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.participantId, payload.participantId)
        assertEquals(event.userId, payload.userId)
        assertEquals(event.roleBeforeLeave, payload.roleBeforeLeave)
        assertEquals(eventTime.epochSecond, payload.leftAt.seconds)
        assertEquals(eventTime.nano, payload.leftAt.nanos)
    }

    @Test
    fun `호스트 퇴장 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = HostLeft(
            roomId = "room-id",
            previousHostParticipantId = "previous-participant-id",
            previousHostUserId = "previous-user-id",
            newHostParticipantId = "new-participant-id",
            newHostUserId = "new-user-id",
            transferredAt = eventTime,
        )

        val payload: HostLeftPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.previousHostParticipantId, payload.previousHostParticipantId)
        assertEquals(event.previousHostUserId, payload.previousHostUserId)
        assertEquals(event.newHostParticipantId, payload.newHostParticipantId)
        assertEquals(event.newHostUserId, payload.newHostUserId)
        assertEquals(eventTime.epochSecond, payload.transferredAt.seconds)
        assertEquals(eventTime.nano, payload.transferredAt.nanos)
    }

    @Test
    fun `호스트 변경 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = HostTransferred(
            roomId = "room-id",
            previousHostUserId = "previous-user-id",
            newHostUserId = "new-user-id",
            transferredAt = eventTime,
        )

        val payload: HostTransferredPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.previousHostUserId, payload.previousHostUserId)
        assertEquals(event.newHostUserId, payload.newHostUserId)
        assertEquals(eventTime.epochSecond, payload.transferredAt.seconds)
        assertEquals(eventTime.nano, payload.transferredAt.nanos)
    }

    @Test
    fun `채팅방 정보 변경 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = RoomInfoChanged(
            roomId = "room-id",
            title = "room-title",
            passwordStateTransition = RoomInfoChanged.PasswordStateTransition.ADDED,
            changedAt = eventTime,
        )

        val payload: RoomInfoChangedPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.title, payload.title)
        assertEquals(
            RoomInfoChangedPayload.PasswordStateTransition.ADDED,
            payload.passwordStateTransition,
        )
        assertEquals(eventTime.epochSecond, payload.changedAt.seconds)
        assertEquals(eventTime.nano, payload.changedAt.nanos)
    }

    @Test
    fun `채팅방 정원 변경 도메인 이벤트를 protobuf payload로 변환한다`() {
        val eventTime = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = RoomCapacityChanged(
            roomId = "room-id",
            previousCapacity = 2,
            currentCapacity = 4,
            changedAt = eventTime,
        )

        val payload: RoomCapacityChangedPayload = toProto(event)

        assertEquals(event.roomId, payload.roomId)
        assertEquals(event.previousCapacity, payload.previousCapacity)
        assertEquals(event.currentCapacity, payload.currentCapacity)
        assertEquals(eventTime.epochSecond, payload.changedAt.seconds)
        assertEquals(eventTime.nano, payload.changedAt.nanos)
    }
}
