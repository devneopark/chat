package com.devneopark.chat.lib.infrastructure.event.schema.user.v1

import com.devneopark.chat.lib.domain.user.event.v1.UserProfileChanged as DomainUserProfileChanged
import com.devneopark.chat.lib.infrastructure.event.schema.user.v1.UserProfileChanged as ProtoUserProfileChanged
import com.google.protobuf.Timestamp

/**
 * 사용자 프로필 변경 도메인 이벤트를 protobuf payload로 변환한다.
 *
 * @param event 변환할 사용자 프로필 변경 도메인 이벤트.
 */
fun toProto(event: DomainUserProfileChanged): ProtoUserProfileChanged {
    val timestamp = Timestamp.newBuilder()
        .setSeconds(event.changedAt.epochSecond)
        .setNanos(event.changedAt.nano)
        .build()
    return ProtoUserProfileChanged.newBuilder()
        .setUserId(event.userId)
        .setDisplayName(event.displayName)
        .setChangedAt(timestamp)
        .build()
}