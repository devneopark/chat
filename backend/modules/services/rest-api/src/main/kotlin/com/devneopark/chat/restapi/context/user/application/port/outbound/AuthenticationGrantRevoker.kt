package com.devneopark.chat.restapi.context.user.application.port.outbound

import com.devneopark.chat.lib.domain.user.reference.UserId

/** 사용자의 인증정보를 일괄 폐기하는 응용 계층 계약이다. */
fun interface AuthenticationGrantRevoker {

    /** 사용자에게 발급된 모든 인증정보를 폐기한다. */
    suspend fun revokeAllByUserId(userId: UserId)

}
