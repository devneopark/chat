package com.devneopark.chat.restapi.context.user.application.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

/** User 컨텍스트의 응용 계층에서 발생하는 예외의 공통 기반이다. */
abstract class UserContextException(

    code: String,

    message: String,

    override val cause: Throwable? = null,

) : ExceptionBase(code, message, cause)
