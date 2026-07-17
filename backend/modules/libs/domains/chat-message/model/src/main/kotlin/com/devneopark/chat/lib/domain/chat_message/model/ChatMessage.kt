package com.devneopark.chat.lib.domain.chat_message.model

import com.devneopark.chat.lib.domain.chat_message.reference.ChatMessageId
import com.devneopark.chat.lib.domain.chat_message.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Instant

/**
 * 채팅 메시지의 공통 상태와 규칙을 정의하는 추상 애그리거트 루트.
 *
 * 메시지 식별자, 소속 채팅방, 발신자, 스레드 루트, 전송 시각을 소유한다.
 * 구체적인 메시지 유형은 이 클래스를 확장해 자신의 본문과 행위를 정의한다.
 *
 * @param id 채팅 메시지 식별자.
 * @param roomId 메시지가 속한 채팅방 식별자.
 * @param senderId 메시지를 보낸 채팅방 참여자 식별자.
 * @param threadRoot 스레드의 루트 메시지 식별자. 스레드에 속하지 않으면 `null`.
 * @param sentAt 메시지가 전송된 시각.
 * @throws DomainRuleViolationException 자기 자신을 스레드 루트로 지정한 경우.
 */
abstract class ChatMessage(

    /**
     * 채팅 메시지 식별자.
     */
    val id: ChatMessageId,

    /**
     * 메시지가 속한 채팅방 식별자.
     */
    val roomId: RoomId,

    /**
     * 메시지를 보낸 채팅방 참여자 식별자.
     */
    val senderId: ParticipantId,

    /**
     * 스레드의 루트 메시지 식별자. 스레드에 속하지 않으면 `null`.
     */
    val threadRoot: ChatMessageId?,

    /**
     * 메시지가 전송된 시각.
     */
    val sentAt: Instant

) {

    init {
        checkThreadRoot(threadRoot)
    }

    private fun checkThreadRoot(newThreadRoot: ChatMessageId?) {
        if (newThreadRoot?.value == id.value) {
            val exceptionDefinition = ExceptionDefinition.SELF_THREAD_ROOT_NOT_ALLOWED
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /**
     * 채팅 메시지 애그리거트에서 사용하는 채팅 메시지 식별자 값 객체.
     *
     * @param value 채팅 메시지 식별자 값.
     * @throws DomainRuleViolationException 식별자 값이 빈 문자열인 경우.
     */
    class Id(

        /**
         * 채팅 메시지 식별자 값.
         */
        override val value: String

    ) : ChatMessageId {

        init {
            checkValue(value)
        }

        companion object {

            /**
             * 문자열 값으로 채팅 메시지 식별자를 생성한다.
             *
             * @param value 채팅 메시지 식별자 값.
             * @return 생성된 채팅 메시지 식별자.
             * @throws DomainRuleViolationException 식별자 값이 빈 문자열인 경우.
             */
            fun from(value: String): Id {
                return Id(value)
            }

        }

        internal fun checkValue(value: String) {
            if (value.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.CHAT_MESSAGE_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
