package com.devneopark.chat.lib.infrastructure.event.schema.user.v1

import com.devneopark.chat.lib.domain.user.event.v1.UserProfileChanged
import com.devneopark.chat.lib.infrastructure.event.schema.user.v1.UserProfileChanged as UserProfileChangedPayload
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileChangedMapperTest {

    @Test
    fun `사용자 프로필 변경 도메인 이벤트를 protobuf payload로 변환한다`() {
        val changedAt = Instant.parse("2026-10-04T12:34:56.123456789Z")
        val event = UserProfileChanged(
            userId = "user-id",
            displayName = "display-name",
            changedAt = changedAt,
        )

        val payload: UserProfileChangedPayload = toProto(event)

        assertEquals(event.userId, payload.userId)
        assertEquals(event.displayName, payload.displayName)
        assertEquals(changedAt.epochSecond, payload.changedAt.seconds)
        assertEquals(changedAt.nano, payload.changedAt.nanos)
    }
}
