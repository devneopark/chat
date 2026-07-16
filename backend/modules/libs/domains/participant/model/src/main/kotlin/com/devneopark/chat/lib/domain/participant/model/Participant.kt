package com.devneopark.chat.lib.domain.participant.model

import com.devneopark.chat.lib.domain.participant.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 채팅방 참여자를 나타내는 애그리거트 루트.
 *
 * 참여자는 독립 식별자, 소속 채팅방과 사용자, 참여자 역할, 입장 시각을 소유한다.
 * 역할 변경은 [promoteToHost]와 [demoteToGuest]를 통해 수행한다.
 *
 * @param id 참여자 식별자.
 * @param roomId 참여자가 속한 채팅방 식별자.
 * @param userId 참여자로 입장한 사용자 식별자.
 * @param role 참여자의 초기 역할.
 * @param joinedAt 참여자가 채팅방에 입장한 시각.
 */
class Participant(

    /**
     * 참여자 식별자.
     */
    val id: ParticipantId,

    /**
     * 참여자가 속한 채팅방 식별자.
     */
    val roomId: RoomId,

    /**
     * 참여자로 입장한 사용자 식별자.
     */
    val userId: UserId,

    role: ParticipantRole,

    /**
     * 참여자가 채팅방에 입장한 시각.
     */
    val joinedAt: Instant = Clock.System.now()

) {

    /**
     * 참여자의 현재 역할.
     */
    var role = role
        private set

    /**
     * 참여자를 호스트로 승격한다.
     */
    fun promoteToHost() {
        this.role = ParticipantRole.HOST
    }

    /**
     * 참여자를 일반 참여자로 강등한다.
     */
    fun demoteToGuest() {
        this.role = ParticipantRole.GUEST
    }

    /**
     * Participant 애그리거트에서 사용하는 참여자 식별자 값 객체.
     *
     * @param value 참여자 식별자 값.
     * @throws DomainRuleViolationException 참여자 식별자 값이 참여자 도메인 규칙을 만족하지 않는 경우.
     */
    class Id(

        /**
         * 참여자 식별자 값.
         */
        override val value: String

    ) : ParticipantId {

        init {
            checkValue(value)
        }

        companion object {

            /**
             * 문자열 값으로 참여자 식별자를 생성한다.
             *
             * @param value 참여자 식별자 값.
             * @return 생성된 참여자 식별자.
             * @throws DomainRuleViolationException 참여자 식별자 값이 참여자 도메인 규칙을 만족하지 않는 경우.
             */
            fun from(value: String): Id {
                return Id(value)
            }

        }

        private fun checkValue(value: String) {
            if (value.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.PARTICIPANT_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
