package com.devneopark.chat.lib.domain.room.model

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 채팅방 메타데이터를 관리하는 애그리거트 루트.
 *
 * 채팅방 식별자와 호스트 사용자, 제목, 비밀번호 해시를 소유한다.
 * 채팅방 정원과 참여 상태는 각각 AdmissionSlot과 Participant 애그리거트에서 관리한다.
 *
 * @param id 채팅방 식별자.
 * @param hostUserId 채팅방 호스트의 사용자 식별자.
 * @param title 채팅방 제목.
 * @param passwordHash 채팅방 비밀번호 해시. 비밀번호가 없는 채팅방이면 `null`.
 * @throws DomainRuleViolationException 제목 또는 비밀번호 해시가 채팅방 도메인 규칙을 만족하지 않는 경우.
 */
class Room(

    /**
     * 채팅방 식별자.
     */
    val id: RoomId,

    hostUserId: UserId,

    title: String,

    passwordHash: String?

) {

    /**
     * 채팅방 호스트의 사용자 식별자.
     */
    var hostUserId: UserId = hostUserId
        private set

    /**
     * 채팅방 제목.
     */
    var title: String = title
        private set

    /**
     * 채팅방 비밀번호 해시. 비밀번호가 없는 채팅방이면 `null`.
     */
    var passwordHash: String? = passwordHash
        private set

    init {
        checkTitle(title)
        checkPasswordHash(passwordHash)
    }

    /**
     * 채팅방 호스트를 변경한다.
     *
     * 대상 사용자가 채팅방 Participant인지 확인하는 작업은 호출 계층에서 선행해야 한다.
     *
     * @param newHostId 새로운 호스트의 사용자 식별자.
     */
    fun changeHost(newHostId: UserId) {
        this.hostUserId = newHostId
    }

    /**
     * 채팅방 제목을 변경한다.
     *
     * @param newTitle 새로운 채팅방 제목.
     * @throws DomainRuleViolationException 새로운 제목이 채팅방 도메인 규칙을 만족하지 않는 경우.
     */
    fun changeTitle(newTitle: String) {
        checkTitle(newTitle)
        this.title = newTitle
    }

    /**
     * 채팅방 비밀번호 해시를 변경한다.
     *
     * `null`을 전달하면 채팅방 비밀번호를 제거한다.
     *
     * @param newPasswordHash 새로운 비밀번호 해시. 비밀번호를 제거하려면 `null`.
     * @throws DomainRuleViolationException 새로운 비밀번호 해시가 채팅방 도메인 규칙을 만족하지 않는 경우.
     */
    fun changePasswordHash(newPasswordHash: String?) {
        checkPasswordHash(newPasswordHash)
        this.passwordHash = newPasswordHash
    }

    private fun checkTitle(newTitle: String) {
        if (newTitle.isBlank() || newTitle.length > 50) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    private fun checkPasswordHash(newPasswordHash: String?) {
        if (newPasswordHash == null) {
            return
        }
        if (newPasswordHash.isBlank()) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_PASSWORD
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /**
     * Room 애그리거트에서 사용하는 채팅방 식별자 값 객체.
     *
     * @param value 채팅방 식별자 값.
     * @throws DomainRuleViolationException 채팅방 식별자 값이 채팅방 도메인 규칙을 만족하지 않는 경우.
     */
    class Id(

        /**
         * 채팅방 식별자 값.
         */
        override val value: String

    ) : RoomId {

        init {
            checkValue(value)
        }

        companion object {

            /**
             * 문자열 값으로 채팅방 식별자를 생성한다.
             *
             * @param value 채팅방 식별자 값.
             * @return 생성된 채팅방 식별자.
             * @throws DomainRuleViolationException 채팅방 식별자 값이 채팅방 도메인 규칙을 만족하지 않는 경우.
             */
            fun from(value: String): Id {
                return Id(value)
            }

        }

        internal fun checkValue(value: String) {
            if (value.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.ROOM_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

    /**
     * Room 애그리거트에서 호스트 사용자를 참조할 때 사용하는 식별자 값 객체.
     *
     * @param value 호스트 사용자 식별자 값.
     * @throws DomainRuleViolationException 호스트 사용자 식별자 값이 채팅방 도메인 규칙을 만족하지 않는 경우.
     */
    class HostId(

        /**
         * 호스트 사용자 식별자 값.
         */
        override val value: String

    ) : UserId {

        init {
            checkValue(value)
        }

        companion object {

            /**
             * 문자열 값으로 호스트 사용자 식별자를 생성한다.
             *
             * @param value 호스트 사용자 식별자 값.
             * @return 생성된 호스트 사용자 식별자.
             * @throws DomainRuleViolationException 호스트 사용자 식별자 값이 채팅방 도메인 규칙을 만족하지 않는 경우.
             */
            fun from(value: String): HostId {
                return HostId(value)
            }

        }

        internal fun checkValue(value: String) {
            if (value.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.HOST_USER_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
