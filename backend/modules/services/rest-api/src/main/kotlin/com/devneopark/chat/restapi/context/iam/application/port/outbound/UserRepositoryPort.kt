package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.user.model.User

interface UserRepositoryPort {

    // Create
    suspend fun insert(user: User): User

    // Read
    suspend fun existsByPrincipal(principal: String): Boolean

    suspend fun findByPrincipal(principal: String): User?

    // Update

    // Delete

}