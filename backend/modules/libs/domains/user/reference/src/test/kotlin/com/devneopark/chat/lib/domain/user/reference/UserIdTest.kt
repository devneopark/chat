package com.devneopark.chat.lib.domain.user.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class UserIdTest {

    @Test
    fun `given user id implementation when value is read then original value is exposed`() {
        // given
        val value = "user-1"
        val userId: UserId = TestUserId(value)

        // when
        val actualValue = userId.value

        // then
        assertEquals(value, actualValue)
    }

    private data class TestUserId(
        override val value: String
    ) : UserId

}
