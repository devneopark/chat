package com.devneopark.chat.lib.shared.domain.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

/**
 * 도메인 규칙을 만족하지 못해 요청을 처리할 수 없을 때 사용하는 예외.
 *
 * 도메인 모델 또는 도메인 서비스가 비즈니스 불변식을 보호하기 위해 던진다.
 *
 * @param code 도메인 규칙 위반을 식별하는 예외 코드.
 * @param message 도메인 규칙 위반 내용을 설명하는 메시지.
 */
class DomainRuleViolationException(
    code: String,
    message: String
) : ExceptionBase(code, message)
