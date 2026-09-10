package com.devneopark.chat.restapi.context.user.application.exception

private const val CODE = "2-002-001"
private const val MESSAGE = "User not found."

class UserNotFoundException(

    override val cause: Throwable? = null

) : UserContextException(CODE, MESSAGE, cause)