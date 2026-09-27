package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-008"
private const val MESSAGE = "Room is full."

/** 입장에 사용할 수 있는 빈 슬롯이 없음을 나타낸다. */
class RoomFullException(

    override val cause: Throwable? = null

) : RoomContextException(CODE, MESSAGE, cause)
