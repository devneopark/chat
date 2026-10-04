package com.devneopark.chat.lib.infrastructure.event.schema.room.v1

import com.devneopark.chat.lib.domain.room.event.v1.HostLeft as DomainHostLeft
import com.devneopark.chat.lib.domain.room.event.v1.HostTransferred as DomainHostTransferred
import com.devneopark.chat.lib.domain.room.event.v1.ParticipantJoined as DomainParticipantJoined
import com.devneopark.chat.lib.domain.room.event.v1.ParticipantLeft as DomainParticipantLeft
import com.devneopark.chat.lib.domain.room.event.v1.RoomCapacityChanged as DomainRoomCapacityChanged
import com.devneopark.chat.lib.domain.room.event.v1.RoomClosed as DomainRoomClosed
import com.devneopark.chat.lib.domain.room.event.v1.RoomInfoChanged as DomainRoomInfoChanged
import com.devneopark.chat.lib.domain.room.event.v1.RoomOpened as DomainRoomOpened
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.HostLeft as ProtoHostLeft
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.HostTransferred as ProtoHostTransferred
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.ParticipantJoined as ProtoParticipantJoined
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.ParticipantLeft as ProtoParticipantLeft
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomCapacityChanged as ProtoRoomCapacityChanged
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomClosed as ProtoRoomClosed
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomInfoChanged as ProtoRoomInfoChanged
import com.devneopark.chat.lib.infrastructure.event.schema.room.v1.RoomOpened as ProtoRoomOpened
import com.google.protobuf.Timestamp
import java.time.Instant

/**
 * 채팅방 생성 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainRoomOpened): ProtoRoomOpened =
    ProtoRoomOpened.newBuilder()
        .setRoomId(event.roomId)
        .setHostParticipantId(event.hostParticipantId)
        .setHostUserId(event.hostUserId)
        .setOpenedAt(toTimestamp(event.openedAt))
        .build()

/**
 * 채팅방 폐쇄 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainRoomClosed): ProtoRoomClosed =
    ProtoRoomClosed.newBuilder()
        .setRoomId(event.roomId)
        .setHostParticipantId(event.hostParticipantId)
        .setHostUserId(event.hostUserId)
        .setClosedAt(toTimestamp(event.closedAt))
        .build()

/**
 * 참여자 입장 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainParticipantJoined): ProtoParticipantJoined =
    ProtoParticipantJoined.newBuilder()
        .setRoomId(event.roomId)
        .setParticipantId(event.participantId)
        .setUserId(event.userId)
        .setRole(event.role)
        .setJoinedAt(toTimestamp(event.joinedAt))
        .build()

/**
 * 참여자 퇴장 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainParticipantLeft): ProtoParticipantLeft =
    ProtoParticipantLeft.newBuilder()
        .setRoomId(event.roomId)
        .setParticipantId(event.participantId)
        .setUserId(event.userId)
        .setRoleBeforeLeave(event.roleBeforeLeave)
        .setLeftAt(toTimestamp(event.leftAt))
        .build()

/**
 * 호스트 퇴장 및 호스트 승계 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainHostLeft): ProtoHostLeft =
    ProtoHostLeft.newBuilder()
        .setRoomId(event.roomId)
        .setPreviousHostParticipantId(event.previousHostParticipantId)
        .setPreviousHostUserId(event.previousHostUserId)
        .setNewHostParticipantId(event.newHostParticipantId)
        .setNewHostUserId(event.newHostUserId)
        .setTransferredAt(toTimestamp(event.transferredAt))
        .build()

/**
 * 호스트 변경 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainHostTransferred): ProtoHostTransferred =
    ProtoHostTransferred.newBuilder()
        .setRoomId(event.roomId)
        .setPreviousHostUserId(event.previousHostUserId)
        .setNewHostUserId(event.newHostUserId)
        .setTransferredAt(toTimestamp(event.transferredAt))
        .build()

/**
 * 채팅방 정보 변경 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainRoomInfoChanged): ProtoRoomInfoChanged =
    ProtoRoomInfoChanged.newBuilder()
        .setRoomId(event.roomId)
        .setTitle(event.title)
        .setPasswordStateTransition(toPasswordStateTransition(event))
        .setChangedAt(toTimestamp(event.changedAt))
        .build()

/**
 * 채팅방 정원 변경 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 도메인 이벤트.
 */
fun toProto(event: DomainRoomCapacityChanged): ProtoRoomCapacityChanged =
    ProtoRoomCapacityChanged.newBuilder()
        .setRoomId(event.roomId)
        .setPreviousCapacity(event.previousCapacity)
        .setCurrentCapacity(event.currentCapacity)
        .setChangedAt(toTimestamp(event.changedAt))
        .build()

private fun toTimestamp(instant: Instant): Timestamp =
    Timestamp.newBuilder()
        .setSeconds(instant.epochSecond)
        .setNanos(instant.nano)
        .build()

private fun toPasswordStateTransition(
    event: DomainRoomInfoChanged,
): ProtoRoomInfoChanged.PasswordStateTransition =
    when (event.passwordStateTransition) {
        DomainRoomInfoChanged.PasswordStateTransition.ADDED -> ProtoRoomInfoChanged.PasswordStateTransition.ADDED
        DomainRoomInfoChanged.PasswordStateTransition.REMOVED -> ProtoRoomInfoChanged.PasswordStateTransition.REMOVED
        DomainRoomInfoChanged.PasswordStateTransition.NONE -> ProtoRoomInfoChanged.PasswordStateTransition.NONE
    }
