package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-001"
private const val MESSAGE = "Principal duplicated."

class DuplicatedPrincipalException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
