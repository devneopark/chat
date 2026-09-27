package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-007"
private const val MESSAGE = "Wrong room password."

/** 보호된 채팅방에 입력한 비밀번호가 일치하지 않음을 나타낸다. */
class WrongRoomPasswordException(

    override val cause: Throwable? = null

) : RoomContextException(CODE, MESSAGE, cause)
