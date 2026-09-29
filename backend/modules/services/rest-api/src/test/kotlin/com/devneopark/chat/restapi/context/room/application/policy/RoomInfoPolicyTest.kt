package com.devneopark.chat.restapi.context.room.application.policy

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomInfoPolicyTest {

    private val policy = RoomInfoPolicy(
        titleRegex = Regex("[A-Za-z0-9 ]{1,20}"),
        passwordRegex = Regex("[A-Za-z0-9]{8,20}")
    )

    @Test
    fun `허용된 title이면 검증을 통과한다`() {
        policy.validateTitle("Chat Room")
    }

    @Test
    fun `허용되지 않은 title이면 도메인 규칙 위반 예외를 던진다`() {
        val exception = assertFailsWith<DomainRuleViolationException> {
            policy.validateTitle("Invalid#Room")
        }

        assertEquals(ExceptionDefinition.INVALID_ROOM_TITLE.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_ROOM_TITLE.message, exception.message)
    }

    @Test
    fun `허용된 password이면 검증을 통과한다`() {
        policy.validatePassword("Password1")
    }

    @Test
    fun `허용되지 않은 password이면 도메인 규칙 위반 예외를 던진다`() {
        val exception = assertFailsWith<DomainRuleViolationException> {
            policy.validatePassword("short")
        }

        assertEquals(ExceptionDefinition.INVALID_ROOM_PASSWORD.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_ROOM_PASSWORD.message, exception.message)
    }

}
