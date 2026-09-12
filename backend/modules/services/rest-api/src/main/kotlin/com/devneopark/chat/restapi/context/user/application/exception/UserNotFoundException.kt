package com.devneopark.chat.restapi.context.user.application.exception

private const val CODE = "2-002-001"
private const val MESSAGE = "User not found."

/** 요청한 사용자가 존재하지 않거나 활성 상태가 아님을 나타낸다. */
class UserNotFoundException(

    override val cause: Throwable? = null

) : UserContextException(CODE, MESSAGE, cause)
