package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-002"
private const val MESSAGE = "User not found."

class UserNotFoundException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
