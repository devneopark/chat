package com.devneopark.chat.restapi.context.user.application.policy

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserProfilePolicyTest {

    private val policy = UserProfilePolicy(Regex("[A-Za-z0-9]{2,20}"))

    @Test
    fun `허용된 displayName이면 검증을 통과한다`() {
        policy.validateDisplayName("Neo123")
    }

    @Test
    fun `허용되지 않은 displayName이면 도메인 규칙 위반 예외를 던진다`() {
        val exception = assertFailsWith<DomainRuleViolationException> {
            policy.validateDisplayName("Neo Park")
        }

        assertEquals(ExceptionDefinition.INVALID_USER_DISPLAY_NAME.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_USER_DISPLAY_NAME.message, exception.message)
    }

}
