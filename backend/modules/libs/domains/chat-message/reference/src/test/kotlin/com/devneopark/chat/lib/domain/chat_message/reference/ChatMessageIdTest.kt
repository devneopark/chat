package com.devneopark.chat.lib.domain.chat_message.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMessageIdTest {

    @Test
    fun `given chat message id implementation when value is read then original value is exposed`() {
        // given
        val value = "chat-message-1"
        val chatMessageId: ChatMessageId = TestChatMessageId(value)

        // when
        val actualValue = chatMessageId.value

        // then
        assertEquals(value, actualValue)
    }

    private data class TestChatMessageId(
        override val value: String
    ) : ChatMessageId

}
