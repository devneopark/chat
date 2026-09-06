package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-005"
private const val MESSAGE = "Invalid access credential."

class InvalidAccessCredentialException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
