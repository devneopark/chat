package com.devneopark.chat.lib.domain.room.service

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/**
 * 채팅방 정보 정책 검증기.
 *
 * 외부에서 주입된 정규식을 사용해 제목과 원문 비밀번호가 현재 채팅방 정책을 만족하는지 확인한다.
 * 정책을 만족하지 않는 값에는 채팅방 도메인 규칙 위반 예외를 던진다.
 *
 * [com.devneopark.chat.lib.domain.room.model.Room]이 보장하는 필수 불변식과 별개로,
 * 실행 환경에서 구성하는 추가 정책을 검증할 때 사용한다.
 *
 * @param titleRegex 채팅방 제목에 적용할 허용 형식 정규식.
 * @param passwordRegex 채팅방 원문 비밀번호에 적용할 허용 형식 정규식.
 */
class RoomInfoValidator(

    private val titleRegex: Regex,

    private val passwordRegex: Regex

) {

    /**
     * 제목이 채팅방 정보 정책을 만족하는지 확인한다.
     *
     * @param title 검증할 채팅방 제목.
     * @throws DomainRuleViolationException 제목이 [titleRegex]와 일치하지 않는 경우.
     */
    fun validateTitle(title: CharSequence) {
        if (!titleRegex.matches(title)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /**
     * 원문 비밀번호가 채팅방 정보 정책을 만족하는지 확인한다.
     *
     * 비밀번호가 없는 채팅방에서는 이 검증을 호출하지 않는다.
     *
     * @param password 검증할 채팅방 원문 비밀번호.
     * @throws DomainRuleViolationException 비밀번호가 [passwordRegex]와 일치하지 않는 경우.
     */
    fun validatePassword(password: CharSequence) {
        if (!passwordRegex.matches(password)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_PASSWORD
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

}
