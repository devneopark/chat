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

class ChatMessageTest {

    @Test
    fun `given message metadata when chat message is created then values are preserved`() {
        // given
        val id = ChatMessage.Id("chat-message-1")
        val roomId = TestRoomId("room-1")
        val senderId = TestParticipantId("participant-1")
        val threadRoot = TestChatMessageId("chat-message-root")
        val sentAt = Instant.parse("2026-07-17T00:00:00Z")

        // when
        val chatMessage = TestChatMessage(id, roomId, senderId, threadRoot, sentAt)

        // then
        assertSame(id, chatMessage.id)
        assertSame(roomId, chatMessage.roomId)
        assertSame(senderId, chatMessage.senderId)
        assertSame(threadRoot, chatMessage.threadRoot)
        assertEquals(sentAt, chatMessage.sentAt)
    }

    @Test
    fun `given equivalent id as thread root when chat message is created then domain rule violation exception is thrown`() {
        // given
        val id = ChatMessage.Id("chat-message-1")
        val exceptionDefinition = ExceptionDefinition.SELF_THREAD_ROOT_NOT_ALLOWED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            TestChatMessage(
                id,
                TestRoomId("room-1"),
                TestParticipantId("participant-1"),
                TestChatMessageId(id.value),
                Instant.parse("2026-07-17T00:00:00Z")
            )
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    private class TestChatMessage(
        id: ChatMessageId,
        roomId: RoomId,
        senderId: ParticipantId,
        threadRoot: ChatMessageId?,
        sentAt: Instant
    ) : ChatMessage(id, roomId, senderId, threadRoot, sentAt)

    private data class TestChatMessageId(override val value: String) : ChatMessageId

    private data class TestRoomId(override val value: String) : RoomId

    private data class TestParticipantId(override val value: String) : ParticipantId

}
