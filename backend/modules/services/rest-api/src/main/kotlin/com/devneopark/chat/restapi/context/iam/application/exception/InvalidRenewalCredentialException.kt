package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-004"
private const val MESSAGE = "Invalid renewal credential."

/** renewal credential이 없거나 만료되어 재발급할 수 없음을 나타낸다. */
class InvalidRenewalCredentialException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
