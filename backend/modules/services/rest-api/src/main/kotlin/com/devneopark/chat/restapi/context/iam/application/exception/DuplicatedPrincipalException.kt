package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-001"
private const val MESSAGE = "Principal duplicated."

/** principal이 이미 활성 사용자에게 사용 중임을 나타낸다. */
class DuplicatedPrincipalException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
