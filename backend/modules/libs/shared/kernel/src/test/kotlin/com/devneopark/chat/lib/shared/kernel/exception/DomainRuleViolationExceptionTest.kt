package com.devneopark.chat.lib.shared.kernel.exception

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainRuleViolationExceptionTest {

    @Test
    fun `given code and message when exception is created then both values are preserved`() {
        // given
        val code = "1-000-001"
        val message = "domain rule violated"

        // when
        val exception = DomainRuleViolationException(code, message)

        // then
        assertEquals(code, exception.code)
        assertEquals(message, exception.message)
    }

    @Test
    fun `given no cause when exception is created then cause is null`() {
        // given
        val code = "1-000-001"
        val message = "domain rule violated"

        // when
        val exception = DomainRuleViolationException(code, message)

        // then
        assertNull(exception.cause)
    }

    @Test
    fun `given blank code when exception is created then illegal argument exception is thrown`() {
        // given
        val code = " "
        val message = "domain rule violated"

        // when
        val exception = assertFailsWith<IllegalArgumentException> {
            DomainRuleViolationException(code, message)
        }

        // then
        assertEquals("exception code can't be empty", exception.message)
    }

    @Test
    fun `given blank message when exception is created then illegal argument exception is thrown`() {
        // given
        val code = "1-000-001"
        val message = " "

        // when
        val exception = assertFailsWith<IllegalArgumentException> {
            DomainRuleViolationException(code, message)
        }

        // then
        assertEquals("exception message can't be empty", exception.message)
    }

    @Test
    fun `given exception when stack trace is requested then stack trace is empty`() {
        // given
        val code = "1-000-001"
        val message = "domain rule violated"
        val exception = DomainRuleViolationException(code, message)

        // when
        val stackTrace = exception.stackTrace

        // then
        assertTrue(stackTrace.isEmpty())
    }

    @Test
    fun `given exception when suppressed exception is added then suppressed list remains empty`() {
        // given
        val code = "1-000-001"
        val message = "domain rule violated"
        val exception = DomainRuleViolationException(code, message)
        val suppressedException = RuntimeException("suppressed")

        // when
        exception.addSuppressed(suppressedException)

        // then
        assertTrue(exception.suppressed.isEmpty())
    }

    @Test
    fun `given domain rule violation exception when checked by type then it follows exception contract`() {
        // given
        val code = "1-000-001"
        val message = "domain rule violated"
        val exception: Throwable = DomainRuleViolationException(code, message)

        // when
        val exceptionBase = exception as? ExceptionBase
        val runtimeException = exception as? RuntimeException

        // then
        assertNotNull(exceptionBase)
        assertNotNull(runtimeException)
    }

}
