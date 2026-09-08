package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-002"
private const val MESSAGE = "User not found."

/** 요청한 principal에 해당하는 사용자가 없음을 나타낸다. */
class UserNotFoundException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
