package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserIdTest {

    @Test
    fun `given value when user id is created then value is preserved`() {
        // given
        val value = "user-1"

        // when
        val id = User.Id(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given value when user id is created from factory then value is preserved`() {
        // given
        val value = "user-1"

        // when
        val id: UserId = User.Id.from(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given blank value when user id is created then domain rule violation exception is thrown`() {
        // given
        val value = " "
        val exceptionDefinition = ExceptionDefinition.USER_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            User.Id(value)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
