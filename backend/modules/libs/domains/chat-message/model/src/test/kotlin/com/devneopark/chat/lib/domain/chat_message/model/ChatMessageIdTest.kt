package com.devneopark.chat.lib.domain.chat_message.model

import com.devneopark.chat.lib.domain.chat_message.reference.ChatMessageId
import com.devneopark.chat.lib.domain.chat_message.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChatMessageIdTest {

    @Test
    fun `given value when chat message id is created then value is preserved`() {
        // given
        val value = "chat-message-1"

        // when
        val id = ChatMessage.Id(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given value when chat message id is created from factory then value is preserved`() {
        // given
        val value = "chat-message-1"

        // when
        val id: ChatMessageId = ChatMessage.Id.from(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given blank value when chat message id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.CHAT_MESSAGE_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            ChatMessage.Id(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
