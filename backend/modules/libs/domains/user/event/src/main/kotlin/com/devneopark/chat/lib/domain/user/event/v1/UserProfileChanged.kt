package com.devneopark.chat.lib.domain.user.event.v1

import java.time.Instant

/**
 * 사용자의 프로필이 변경되었음을 나타내는 도메인 이벤트.
 *
 * @param userId 프로필이 변경된 사용자의 식별자.
 * @param displayName 변경된 사용자의 표시 이름.
 * @param changedAt 프로필 변경이 발생한 시각.
 */
data class UserProfileChanged(

    /**
     * 프로필이 변경된 사용자의 식별자.
     */
    val userId: String,

    /**
     * 변경된 사용자의 표시 이름.
     */
    val displayName: String,

    /**
     * 프로필 변경이 발생한 시각.
     */
    val changedAt: Instant
)
