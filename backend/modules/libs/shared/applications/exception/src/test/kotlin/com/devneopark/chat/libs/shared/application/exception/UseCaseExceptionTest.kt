package com.devneopark.chat.libs.shared.application.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UseCaseExceptionTest {

    @Test
    fun `given conflict exception when it is created then common code and message are preserved`() {
        // when
        val exception = ApplicationConflictException()

        // then
        assertEquals("2-000-001", exception.code)
        assertEquals("Conflict occurred.", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `given unavailable exception when it is created then common code and message are preserved`() {
        // when
        val exception = ApplicationUnavailableException()

        // then
        assertEquals("2-000-002", exception.code)
        assertEquals("Temporary unavailable.", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `given cause when a common application exception is created then cause is preserved`() {
        // given
        val cause = IllegalStateException("dependency failed")

        // when
        val conflict = ApplicationConflictException(cause)
        val unavailable = ApplicationUnavailableException(cause)

        // then
        assertSame(cause, conflict.cause)
        assertSame(cause, unavailable.cause)
    }

    @Test
    fun `given use case exception when checked by type then it follows the common exception contract`() {
        // given
        val exception: Throwable = ApplicationConflictException()

        // when
        val exceptionBase = exception as? ExceptionBase
        val useCaseFailure = exception as? ApplicationFailureException
        val runtimeException = exception as? RuntimeException

        // then
        assertTrue(exceptionBase != null)
        assertTrue(useCaseFailure != null)
        assertTrue(runtimeException != null)
    }

    @Test
    fun `given a consumer specific exception when it extends the common base then its code and message are preserved`() {
        // when
        val exception = ConsumerSpecificApplicationException()

        // then
        assertEquals("2-001-001", exception.code)
        assertEquals("User principal already exists.", exception.message)
    }

    private class ConsumerSpecificApplicationException : ApplicationFailureException(
        code = "2-001-001",
        message = "User principal already exists."
    )

}
