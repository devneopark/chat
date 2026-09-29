package com.devneopark.chat.restapi.context.user.application.policy

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserCredentialPolicyTest {

    private val policy = UserCredentialPolicy(
        principalRegex = Regex("[a-z][a-z0-9_]{2,15}"),
        passwordRegex = Regex("[A-Za-z0-9!@#]{8,20}")
    )

    @Test
    fun `허용된 principal이면 검증을 통과한다`() {
        policy.validatePrincipal("neo_123")
    }

    @Test
    fun `허용되지 않은 principal이면 도메인 규칙 위반 예외를 던진다`() {
        val exception = assertFailsWith<DomainRuleViolationException> {
            policy.validatePrincipal("Neo-123")
        }

        assertEquals(ExceptionDefinition.INVALID_USER_PRINCIPAL.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_USER_PRINCIPAL.message, exception.message)
    }

    @Test
    fun `허용된 password이면 검증을 통과한다`() {
        policy.validatePassword("Passw0rd!")
    }

    @Test
    fun `허용되지 않은 password이면 도메인 규칙 위반 예외를 던진다`() {
        val exception = assertFailsWith<DomainRuleViolationException> {
            policy.validatePassword("short")
        }

        assertEquals(ExceptionDefinition.INVALID_USER_PASSWORD.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_USER_PASSWORD.message, exception.message)
    }

}
