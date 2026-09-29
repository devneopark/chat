package com.devneopark.chat.restapi.context.user.application.port.outbound

import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.time.Instant

/** User 컨텍스트의 사용자 등록, 활성 사용자 조회와 사용자 상태·프로필 변경을 담당하는 영속성 계약이다. */
interface UserRepositoryPort {

    // Create
    /** 사용자를 저장한다. */
    suspend fun insert(user: User): User

    // Read
    /** 활성 사용자 중 principal이 존재하는지 확인한다. */
    suspend fun existsByPrincipal(principal: String): Boolean

    /** 탈퇴하지 않은 사용자를 잠금 없이 식별자로 조회한다. 없으면 `null`을 반환한다. */
    suspend fun findById(id: UserId): User?

    /** 탈퇴하지 않은 사용자를 식별자로 조회하고 쓰기 잠금을 획득한다. 없으면 `null`을 반환한다. */
    suspend fun findByIdForUpdate(id: UserId): User?

    // Update
    /** 탈퇴하지 않은 사용자의 프로필 표시 이름을 변경한다. */
    suspend fun updateProfile(user: User)

    /** 탈퇴하지 않은 사용자를 탈퇴 상태로 변경한다. */
    suspend fun withdraw(userId: UserId, withdrawnAt: Instant)

    // Delete

}
