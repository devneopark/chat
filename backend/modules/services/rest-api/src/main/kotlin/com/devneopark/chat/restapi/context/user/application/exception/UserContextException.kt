package com.devneopark.chat.restapi.context.user.application.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

abstract class UserContextException(

    code: String,

    message: String,

    override val cause: Throwable? = null,

) : ExceptionBase(code, message, cause)
