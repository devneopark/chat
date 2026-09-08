package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.user.model.User

/** 사용자 aggregate의 영속화와 principal 조회를 담당하는 계약이다. */
interface UserRepositoryPort {

    // Create
    /** 사용자를 저장한다. */
    suspend fun insert(user: User): User

    // Read
    /** 활성 사용자 중 principal이 존재하는지 확인한다. */
    suspend fun existsByPrincipal(principal: String): Boolean

    /** principal로 사용자를 조회한다. */
    suspend fun findByPrincipal(principal: String): User?

    // Update

    // Delete

}
