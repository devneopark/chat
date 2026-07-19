package com.devneopark.chat.lib.domain.chat_message.model

import com.devneopark.chat.lib.domain.chat_message.reference.ChatMessageId
import com.devneopark.chat.lib.domain.chat_message.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Instant

class TextMessageTest {

    @Test
    fun `given text message values when text message is created then values are preserved`() {
        // given
        val id = ChatMessage.Id("chat-message-1")
        val roomId = TestRoomId("room-1")
        val senderId = TestParticipantId("participant-1")
        val threadRoot = TestChatMessageId("chat-message-root")
        val sentAt = Instant.parse("2026-07-17T00:00:00Z")
        val message = "Hello Neo"
        val mention = Mention(TestParticipantId("participant-2"), 6, 9)
        val replyTo = TestChatMessageId("chat-message-reply")

        // when
        val textMessage = TextMessage(
            id,
            roomId,
            senderId,
            threadRoot,
            sentAt,
            message,
            listOf(mention),
            replyTo
        )

        // then
        assertSame(id, textMessage.id)
        assertSame(roomId, textMessage.roomId)
        assertSame(senderId, textMessage.senderId)
        assertSame(threadRoot, textMessage.threadRoot)
        assertEquals(sentAt, textMessage.sentAt)
        assertEquals(message, textMessage.message)
        assertEquals(listOf(mention), textMessage.mentions)
        assertSame(replyTo, textMessage.replyTo)
    }

    @Test
    fun `given mutable mentions when text message is created then later external changes do not affect mentions`() {
        // given
        val mention = Mention(TestParticipantId("participant-2"), 0, 1)
        val mentions = mutableListOf(mention)
        val textMessage = createTextMessage("Hello", mentions)

        // when
        mentions.clear()

        // then
        assertEquals(listOf(mention), textMessage.mentions)
    }

    @Test
    fun `given blank message when text message is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.MESSAGE_PAYLOAD_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            createTextMessage(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given mention outside message when text message is created then domain rule violation exception is thrown`() {
        // given
        val mention = Mention(TestParticipantId("participant-2"), 0, 6)
        val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_RANGE

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            createTextMessage("Hello", listOf(mention))
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given overlapping mentions when text message is created then domain rule violation exception is thrown`() {
        // given
        val mentions = listOf(
            Mention(TestParticipantId("participant-2"), 0, 3),
            Mention(TestParticipantId("participant-3"), 2, 5)
        )
        val exceptionDefinition = ExceptionDefinition.OVERLAPPING_MENTION_RANGES

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            createTextMessage("Hello", mentions)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given adjacent mentions when text message is created then mentions are preserved`() {
        // given
        val mentions = listOf(
            Mention(TestParticipantId("participant-2"), 0, 2),
            Mention(TestParticipantId("participant-3"), 2, 5)
        )

        // when
        val textMessage = createTextMessage("Hello", mentions)

        // then
        assertEquals(mentions, textMessage.mentions)
    }

    @Test
    fun `given equivalent id as reply target when text message is created then domain rule violation exception is thrown`() {
        // given
        val id = ChatMessage.Id("chat-message-1")
        val exceptionDefinition = ExceptionDefinition.SELF_REPLY_NOT_ALLOWED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            TextMessage(
                id = id,
                roomId = TestRoomId("room-1"),
                senderId = TestParticipantId("participant-1"),
                message = "Hello",
                replyTo = TestChatMessageId(id.value)
            )
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given new message and mentions when text message is edited then values are updated`() {
        // given
        val textMessage = createTextMessage("Hello")
        val newMessage = "Hello Neo"
        val newMention = Mention(TestParticipantId("participant-2"), 6, 9)

        // when
        textMessage.edit(newMessage, listOf(newMention))

        // then
        assertEquals(newMessage, textMessage.message)
        assertEquals(listOf(newMention), textMessage.mentions)
    }

    @Test
    fun `given blank message when text message edit is attempted then exception is thrown and original values are retained`() {
        // given
        val originalMention = Mention(TestParticipantId("participant-2"), 0, 1)
        val textMessage = createTextMessage("Hello", listOf(originalMention))
        val exceptionDefinition = ExceptionDefinition.MESSAGE_PAYLOAD_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            textMessage.edit(" ", emptyList())
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals("Hello", textMessage.message)
        assertEquals(listOf(originalMention), textMessage.mentions)
    }

    @Test
    fun `given invalid mentions when text message edit is attempted then exception is thrown and original values are retained`() {
        // given
        val originalMention = Mention(TestParticipantId("participant-2"), 0, 1)
        val textMessage = createTextMessage("Hello", listOf(originalMention))
        val invalidMentions = listOf(
            Mention(TestParticipantId("participant-3"), 0, 3),
            Mention(TestParticipantId("participant-4"), 2, 5)
        )
        val exceptionDefinition = ExceptionDefinition.OVERLAPPING_MENTION_RANGES

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            textMessage.edit("World", invalidMentions)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals("Hello", textMessage.message)
        assertEquals(listOf(originalMention), textMessage.mentions)
    }

    @Test
    fun `given mutable mentions when text message is edited then later external changes do not affect mentions`() {
        // given
        val textMessage = createTextMessage("Hello")
        val mention = Mention(TestParticipantId("participant-2"), 0, 1)
        val newMentions = mutableListOf(mention)

        // when
        textMessage.edit("World", newMentions)
        newMentions.clear()

        // then
        assertEquals(listOf(mention), textMessage.mentions)
    }

    private fun createTextMessage(
        message: String,
        mentions: List<Mention> = emptyList()
    ): TextMessage {
        return TextMessage(
            id = ChatMessage.Id("chat-message-1"),
            roomId = TestRoomId("room-1"),
            senderId = TestParticipantId("participant-1"),
            message = message,
            mentions = mentions
        )
    }

    private data class TestChatMessageId(override val value: String) : ChatMessageId

    private data class TestRoomId(override val value: String) : RoomId

    private data class TestParticipantId(override val value: String) : ParticipantId

}
