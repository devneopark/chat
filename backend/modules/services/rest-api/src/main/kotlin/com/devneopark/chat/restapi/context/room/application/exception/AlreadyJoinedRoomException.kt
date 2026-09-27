package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-009"
private const val MESSAGE = "Already joined room."

/** 요청 사용자가 이미 활성 참여자로 등록된 채팅방임을 나타낸다. */
class AlreadyJoinedRoomException(

    override val cause: Throwable? = null

) : RoomContextException(CODE, MESSAGE, cause)
