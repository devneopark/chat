package com.devneopark.chat.lib.domain.admission_slot.model

import com.devneopark.chat.lib.domain.admission_slot.reference.AdmissionSlotId
import com.devneopark.chat.lib.domain.admission_slot.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 채팅방 정원 한 자리를 나타내는 애그리거트 루트.
 *
 * 슬롯은 채팅방 내부 번호와 현재 점유 참여자를 소유한다.
 * 점유자는 [assign]과 [revoke]를 통해서만 변경한다.
 *
 * @param id 슬롯 식별자.
 * @param occupant 현재 슬롯을 점유한 참여자 식별자. 비어 있는 슬롯이면 `null`.
 */
class AdmissionSlot(

    /**
     * 슬롯 식별자.
     */
    val id: AdmissionSlotId,

    occupant: ParticipantId? = null

) {

    /**
     * 현재 슬롯을 점유한 참여자 식별자. 비어 있는 슬롯이면 `null`.
     */
    var occupant: ParticipantId? = occupant
        private set

    /**
     * 비어 있는 슬롯을 참여자에게 할당한다.
     *
     * @param newOccupant 새로 슬롯을 점유할 참여자 식별자.
     * @throws DomainRuleViolationException 이미 점유된 슬롯에 참여자를 할당하려는 경우.
     */
    fun assign(newOccupant: ParticipantId) {
        if (this.occupant != null) {
            val exceptionDefinition = ExceptionDefinition.ALREADY_OCCUPIED
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
        this.occupant = newOccupant
    }

    /**
     * 슬롯 점유를 해제한다.
     *
     * 슬롯이 비어 있으면 상태를 그대로 비어 있는 상태로 유지한다.
     *
     * @param occupant 슬롯 점유를 해제할 참여자 식별자.
     * @throws DomainRuleViolationException 현재 점유자와 해제 요청 참여자가 다른 경우.
     */
    fun revoke(occupant: ParticipantId) {
        if (this.occupant != null) {
            val currentOccupant = this.occupant!!
            val isRoomIdMatched = currentOccupant.roomId == occupant.roomId
            val isUserIdMatched = currentOccupant.userId == occupant.userId
            if (!isRoomIdMatched || !isUserIdMatched) {
                val exceptionDefinition = ExceptionDefinition.OCCUPANT_MISMATCH
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }
        this.occupant = null
    }

    /**
     * AdmissionSlot 애그리거트에서 사용하는 슬롯 식별자 값 객체.
     *
     * @param roomId 슬롯이 속한 채팅방 식별자 값.
     * @param number 채팅방 내부에서 슬롯을 구분하는 번호 값.
     * @throws DomainRuleViolationException 채팅방 식별자 또는 슬롯 번호가 슬롯 도메인 규칙을 만족하지 않는 경우.
     */
    class Id(

        /**
         * 슬롯이 속한 채팅방 식별자 값.
         */
        override val roomId: String,

        /**
         * 채팅방 내부에서 슬롯을 구분하는 번호 값.
         */
        override val number: Int

    ) : AdmissionSlotId {

        init {
            checkRoomId(roomId)
            checkNumber(number)
        }

        companion object {

            /**
             * 채팅방 식별자와 슬롯 번호로 슬롯 식별자를 생성한다.
             *
             * @param roomId 슬롯이 속한 채팅방 식별자 값.
             * @param number 채팅방 내부에서 슬롯을 구분하는 번호 값.
             * @return 생성된 슬롯 식별자.
             * @throws DomainRuleViolationException 채팅방 식별자 또는 슬롯 번호가 슬롯 도메인 규칙을 만족하지 않는 경우.
             */
            fun from(roomId: String, number: Int): Id {
                return Id(roomId, number)
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

        private fun checkNumber(number: Int) {
            if (number < 1) {
                val exceptionDefinition = ExceptionDefinition.INVALID_SLOT_NUMBER
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
