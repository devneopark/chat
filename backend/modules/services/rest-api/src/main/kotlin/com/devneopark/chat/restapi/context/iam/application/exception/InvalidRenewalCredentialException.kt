package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-004"
private const val MESSAGE = "Invalid renewal credential."

class InvalidRenewalCredentialException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
