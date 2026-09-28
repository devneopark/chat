package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.user.model.User

/** 인증에 필요한 활성 사용자의 principal 조회를 담당하는 계약이다. */
interface IamUserRepositoryPort {

    // Create

    // Read
    /** principal로 사용자를 조회한다. */
    suspend fun findByPrincipal(principal: String): User?

    // Update

    // Delete

}
