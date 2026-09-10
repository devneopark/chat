package com.devneopark.chat.restapi.context.user.application.port.outbound

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.reference.UserId

interface UserRepositoryPort {

    // Create

    // Read
    suspend fun findById(id: UserId): User?

    // Update
    suspend fun updateProfile(user: User)

    // Delete

}