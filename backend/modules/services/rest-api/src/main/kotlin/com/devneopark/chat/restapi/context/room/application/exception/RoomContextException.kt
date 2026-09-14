package com.devneopark.chat.restapi.context.room.application.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

abstract class RoomContextException(

    code: String,

    message: String,

    override val cause: Throwable? = null

) : ExceptionBase(code, message, cause)