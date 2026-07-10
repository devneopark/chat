package com.devneopark.chat.lib.domain.participant.model

import com.devneopark.chat.lib.domain.participant.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 채팅방 참여자를 나타내는 애그리거트 루트.
 *
 * 참여자는 채팅방과 사용자로 구성된 복합 식별자, 참여자 역할, 입장 시각을 소유한다.
 * 역할 변경은 [promoteToHost]와 [demoteToGuest]를 통해 수행한다.
 *
 * @param id 참여자 식별자.
 * @param role 참여자의 초기 역할.
 * @param joinedAt 참여자가 채팅방에 입장한 시각.
 */
class Participant(

    /**
     * 참여자 식별자.
     */
    val id: ParticipantId,

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
     * @param roomId 참여자가 속한 채팅방 식별자 값.
     * @param userId 참여자로 입장한 사용자 식별자 값.
     * @throws DomainRuleViolationException 채팅방 식별자 또는 사용자 식별자가 참여자 도메인 규칙을 만족하지 않는 경우.
     */
    class Id(

        /**
         * 참여자가 속한 채팅방 식별자 값.
         */
        override val roomId: String,

        /**
         * 참여자로 입장한 사용자 식별자 값.
         */
        override val userId: String

    ) : ParticipantId {

        init {
            checkRoomId(roomId)
            checkUserId(userId)
        }

        companion object {

            /**
             * 채팅방 식별자와 사용자 식별자로 참여자 식별자를 생성한다.
             *
             * @param roomId 참여자가 속한 채팅방 식별자 값.
             * @param userId 참여자로 입장한 사용자 식별자 값.
             * @return 생성된 참여자 식별자.
             * @throws DomainRuleViolationException 채팅방 식별자 또는 사용자 식별자가 참여자 도메인 규칙을 만족하지 않는 경우.
             */
            fun from(roomId: String, userId: String): Id {
                return Id(roomId, userId)
            }

        }

        private fun checkRoomId(roomId: String) {
            if (roomId.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.ROOM_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

        private fun checkUserId(userId: String) {
            if (userId.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.USER_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
