package com.devneopark.chat.restapi.context.iam.application.exception

private const val CODE = "2-001-005"
private const val MESSAGE = "Invalid access credential."

/** 서명 검증 또는 AuthenticationGrant 화이트리스트 검증에 실패했음을 나타낸다. */
class InvalidAccessCredentialException(

    override val cause: Throwable? = null,

) : IamContextException(CODE, MESSAGE, cause)
