package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-003"
private const val MESSAGE = "Wrong password."

class WrongPasswordException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
