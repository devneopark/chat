package com.devneopark.chat.libs.shared.application.identifier

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HyphenlessUuidGeneratorTest {

    @Test
    fun `given generator when identifier is generated then lowercase hexadecimal UUID without hyphens is returned`() {
        // given
        val generator: IdGenerator = HyphenlessUuidGenerator()

        // when
        val identifier = runSuspend { generator.generate() }

        // then
        assertTrue(identifier.matches(IDENTIFIER_PATTERN))
    }

    @Test
    fun `given generator when identifiers are generated then unique values are returned`() {
        // given
        val generator: IdGenerator = HyphenlessUuidGenerator()

        // when
        val identifiers = List(GENERATION_COUNT) {
            runSuspend { generator.generate() }
        }

        // then
        assertEquals(GENERATION_COUNT, identifiers.toSet().size)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        val completion = CompletableFuture<T>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                result.fold(completion::complete, completion::completeExceptionally)
            }
        })
        return completion.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private companion object {
        val IDENTIFIER_PATTERN = Regex("^[0-9a-f]{32}$")
        const val GENERATION_COUNT = 100
        const val TEST_TIMEOUT_SECONDS = 5L
    }

}
