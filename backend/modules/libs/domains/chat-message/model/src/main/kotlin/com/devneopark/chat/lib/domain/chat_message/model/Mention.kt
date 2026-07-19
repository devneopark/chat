package com.devneopark.chat.lib.domain.chat_message.model

import com.devneopark.chat.lib.domain.chat_message.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 텍스트 메시지에서 특정 채팅방 참여자를 멘션한 범위를 나타내는 값 객체.
 *
 * 멘션 대상은 [participantId]로 식별하며, 표시할 문자열은 메시지 본문의 멘션 범위에서 얻는다.
 * [startInclusive]와 [endExclusive]는 메시지 본문에서 멘션이 표시되는 반개 구간을 나타낸다.
 *
 * @param participantId 멘션 대상인 채팅방 참여자 식별자.
 * @param startInclusive 멘션 범위의 포함되는 시작 인덱스.
 * @param endExclusive 멘션 범위의 포함되지 않는 종료 인덱스.
 * @throws DomainRuleViolationException 인덱스 범위가 유효하지 않은 경우.
 */
class Mention(

    /**
     * 멘션 대상인 채팅방 참여자 식별자.
     */
    val participantId: ParticipantId,

    /**
     * 멘션 범위의 포함되는 시작 인덱스.
     */
    val startInclusive: Int,

    /**
     * 멘션 범위의 포함되지 않는 종료 인덱스.
     */
    val endExclusive: Int

) {

    init {
        checkRange(startInclusive, endExclusive)
    }

    private fun checkRange(startInclusive: Int, endExclusive: Int) {
        if (startInclusive < 0) {
            val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_START_INDEX_NUMBER
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
        if (endExclusive < 0) {
            val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_END_INDEX_NUMBER
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
        if (startInclusive >= endExclusive) {
            val exceptionDefinition = ExceptionDefinition.INVALID_MENTION_RANGE
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
