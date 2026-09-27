package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-002"
private const val MESSAGE = "Capacity out of bound."

class InvalidCapacityException(

    cause: Throwable? = null,

) : RoomContextException(CODE, MESSAGE, cause)