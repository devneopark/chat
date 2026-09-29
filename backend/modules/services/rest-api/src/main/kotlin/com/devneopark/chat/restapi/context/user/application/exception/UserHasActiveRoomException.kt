package com.devneopark.chat.restapi.context.user.application.exception

private const val CODE = "2-002-002"
private const val MESSAGE = "User has active room participation."

/** 활성 채팅방에 참여 중인 사용자의 탈퇴를 거부한다. */
class UserHasActiveRoomException(

    override val cause: Throwable? = null

) : UserContextException(CODE, MESSAGE, cause)
