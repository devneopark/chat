package com.devneopark.chat.restapi.context.user.application.port.outbound

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.reference.UserId

/** User 컨텍스트의 사용자 조회와 프로필 변경을 담당하는 영속성 계약이다. */
interface UserRepositoryPort {

    // Create

    // Read
    /** 탈퇴하지 않은 사용자를 식별자로 조회한다. 없으면 `null`을 반환한다. */
    suspend fun findById(id: UserId): User?

    // Update
    /** 탈퇴하지 않은 사용자의 프로필 표시 이름을 변경한다. */
    suspend fun updateProfile(user: User)

    // Delete

}
