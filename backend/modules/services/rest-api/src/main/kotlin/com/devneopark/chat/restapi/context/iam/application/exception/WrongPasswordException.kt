package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-003"
private const val MESSAGE = "Wrong password."

/** principal은 존재하지만 입력된 password가 일치하지 않음을 나타낸다. */
class WrongPasswordException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
