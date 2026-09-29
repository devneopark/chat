package com.devneopark.chat.restapi.context.room.application.policy

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException

/** 채팅방 제목과 원문 비밀번호에 적용하는 입력 정책이다. */
class RoomInfoPolicy(

    private val titleRegex: Regex,

    private val passwordRegex: Regex

) {

    /** 채팅방 제목이 채팅방 정보 정책을 만족하는지 확인한다. */
    fun validateTitle(title: CharSequence) {
        if (!titleRegex.matches(title)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE
            throw DomainRuleViolationException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }
    }

    /** 채팅방 원문 비밀번호가 채팅방 정보 정책을 만족하는지 확인한다. */
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
