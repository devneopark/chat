package com.devneopark.chat.lib.domain.room.model

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomHostIdTest {

    @Test
    fun `given value when host id is created then value is preserved`() {
        // given
        val value = "user-1"

        // when
        val id = Room.HostId(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given value when host id is created from factory then value is preserved`() {
        // given
        val value = "user-1"

        // when
        val id: UserId = Room.HostId.from(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given blank value when host id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.HOST_USER_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Room.HostId(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
