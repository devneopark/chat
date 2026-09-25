package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-005"
private const val MESSAGE = "Room participant not found."

/** 요청한 채팅방의 활성 참여자를 찾을 수 없음을 나타낸다. */
class RoomParticipantNotFoundException(

    override val cause: Throwable? = null

) : RoomContextException(CODE, MESSAGE, cause)
