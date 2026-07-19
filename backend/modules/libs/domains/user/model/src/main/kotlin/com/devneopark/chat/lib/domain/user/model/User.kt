package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 사용자 애그리거트 루트.
 *
 * 사용자 식별자와 인증 정보, 프로필을 소유하며 사용자 상태 변경을 위한 진입점을 제공한다.
 * 애그리거트 내부 상태 변경은 [changePassword]와 [changeDisplayName]을 통해 수행한다.
 *
 * @param id 사용자 식별자.
 * @param credential 사용자의 인증 정보.
 * @param profile 사용자의 프로필.
 */
class User(

    /**
     * 사용자 식별자.
     */
    val id: UserId,

    /**
     * 사용자의 인증 정보.
     */
    val credential: Credential,

    /**
     * 사용자의 프로필.
     */
    val profile: Profile

) {

    /**
     * 사용자의 비밀번호 해시를 변경한다.
     *
     * @param newPasswordHash 새 비밀번호 해시.
     * @throws DomainRuleViolationException 새 비밀번호 해시가 사용자 도메인 규칙을 만족하지 않는 경우.
     */
    fun changePassword(newPasswordHash: String) {
        credential.changePasswordHash(newPasswordHash)
    }

    /**
     * 사용자의 표시 이름을 변경한다.
     *
     * @param newDisplayName 새 표시 이름.
     * @throws DomainRuleViolationException 새 표시 이름이 사용자 도메인 규칙을 만족하지 않는 경우.
     */
    fun changeDisplayName(newDisplayName: String) {
        profile.changeDisplayName(newDisplayName)
    }

    /**
     * 사용자 애그리거트에서 사용하는 사용자 식별자 값 객체.
     *
     * @param value 사용자 식별자 값.
     * @throws DomainRuleViolationException 사용자 식별자 값이 사용자 도메인 규칙을 만족하지 않는 경우.
     */
    class Id(

        /**
         * 사용자 식별자 값.
         */
        override val value: String

    ) : UserId {

        init {
            checkValue(value)
        }

        companion object {

            /**
             * 문자열 값으로 사용자 식별자를 생성한다.
             *
             * @param value 사용자 식별자 값.
             * @return 생성된 사용자 식별자.
             * @throws DomainRuleViolationException 사용자 식별자 값이 사용자 도메인 규칙을 만족하지 않는 경우.
             */
            fun from(value: String): Id {
                return Id(value)
            }

        }

        internal fun checkValue(value: String) {
            if (value.isBlank()) {
                val exceptionDefinition = ExceptionDefinition.USER_ID_REQUIRED
                throw DomainRuleViolationException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }
        }

    }

}
