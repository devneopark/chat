package com.devneopark.chat.lib.domain.chat_message.model

import com.devneopark.chat.lib.domain.chat_message.reference.ChatMessageId
import com.devneopark.chat.lib.domain.chat_message.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 텍스트 본문과 멘션, 답장 대상을 소유하는 채팅 메시지 애그리거트 루트.
 *
 * 멘션 목록은 외부의 가변 목록 변경으로부터 도메인 상태를 보호하기 위해 복사해 보관한다.
 * 본문과 멘션은 [edit]을 통해 함께 변경한다.
 *
 * @param id 채팅 메시지 식별자.
 * @param roomId 메시지가 속한 채팅방 식별자.
 * @param senderId 메시지를 보낸 채팅방 참여자 식별자.
 * @param threadRootChatMessageId 스레드의 루트 메시지 식별자. 스레드에 속하지 않으면 `null`.
 * @param sentAt 메시지가 전송된 시각. 기본값은 현재 시각이다.
 * @param message 텍스트 메시지 본문.
 * @param mentions 본문에 포함된 멘션 목록.
 * @param replyTo 답장 대상 메시지 식별자. 답장이 아니면 `null`.
 * @throws DomainRuleViolationException 본문이 비어 있거나, 멘션 범위가 유효하지 않거나 겹치거나,
 * 자기 자신을 스레드 루트 또는 답장 대상으로 지정한 경우.
 */
class TextMessage(

    id: ChatMessageId,

    roomId: RoomId,

    senderId: ParticipantId,

    threadRootChatMessageId: ChatMessageId? = null,

    sentAt: Instant = Clock.System.now(),

    message: String,

    mentions: List<Mention> = listOf(),

    replyTo: ChatMessageId? = null

) : ChatMessage(id, roomId, senderId, threadRootChatMessageId, sentAt) {

    /**
     * 텍스트 메시지 본문.
     */
    var message = message
        private set

    /**
     * 본문에 포함된 멘션 목록.
     */
    var mentions = mentions.toList()
        private set

    /**
     * 답장 대상 메시지 식별자. 답장이 아니면 `null`.
     */
    var replyTo = replyTo
        private set

    init {
        checkMessage(message)
        checkMessageAndMention(message, this.mentions)
        checkReplyTo(replyTo)
    }

    /**
     * 텍스트 본문과 멘션 목록을 함께 변경한다.
     *
     * 새 값을 모두 검증한 뒤 상태를 변경하므로, 검증이 실패하면 기존 본문과 멘션을 유지한다.
     *
     * @param newMessage 새로운 텍스트 메시지 본문.
     * @param newMentions 새로운 본문에 포함된 멘션 목록.
     * @throws DomainRuleViolationException 새 본문이 비어 있거나 멘션 범위가 유효하지 않거나 겹치는 경우.
     */
    fun edit(newMessage: String, newMentions: List<Mention>) {
        val copiedMentions = newMentions.toList()
        checkMessage(newMessage)
        checkMessageAndMention(newMessage, copiedMentions)
        this.message = newMessage
        this.mentions = copiedMentions
    }

    private fun checkMessage(newMessage: String) {
        if (newMessage.isBlank()) {
            val exceptionDefinition = ExceptionDefinition.MESSAGE_PAYLOAD_REQUIRED
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    private fun checkMessageAndMention(content: String, mentions: List<Mention>) {
        mentions.forEach { mention ->
            if (
                mention.startInclusive >= mention.endExclusive ||
                mention.endExclusive > content.length
            ) {
                val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_RANGE
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

        mentions
            .sortedBy(Mention::startInclusive)
            .zipWithNext()
            .forEach { (current, next) ->
                if (next.startInclusive < current.endExclusive) {
                    val exceptionDefinition = ExceptionDefinition.OVERLAPPING_MENTION_RANGES
                    throw DomainRuleViolationException(
                        exceptionDefinition.code,
                        exceptionDefinition.message
                    )
                }
            }
    }

    private fun checkReplyTo(newReplyTo: ChatMessageId?) {
        if (newReplyTo?.value == id.value) {
            val exceptionDefinition = ExceptionDefinition.SELF_REPLY_NOT_ALLOWED
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
