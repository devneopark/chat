package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-003"
private const val MESSAGE = "Room not found."

/** 요청한 채팅방이 존재하지 않거나 이미 닫힌 상태임을 나타낸다. */
class RoomNotFoundException(

    override val cause: Throwable? = null

) : RoomContextException(CODE, MESSAGE, cause)
