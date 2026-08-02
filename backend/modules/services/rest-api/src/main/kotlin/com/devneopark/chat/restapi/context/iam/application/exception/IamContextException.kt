package com.devneopark.chat.restapi.context.iam.application.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

class IamContextException(

    code: String,

    message: String,

    override val cause: Throwable? = null,

) : ExceptionBase(code, message, cause)