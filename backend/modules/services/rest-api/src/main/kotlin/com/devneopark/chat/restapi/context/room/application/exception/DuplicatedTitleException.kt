package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-001"
private const val MESSAGE = "Title duplicated."

class DuplicatedTitleException(

    cause: Throwable? = null,

) : RoomContextException(CODE, MESSAGE, cause)