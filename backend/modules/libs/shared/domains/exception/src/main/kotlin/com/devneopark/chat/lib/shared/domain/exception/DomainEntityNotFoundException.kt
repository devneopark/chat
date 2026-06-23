package com.devneopark.chat.lib.shared.domain.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

/**
 * 도메인 엔티티를 찾을 수 없을 때 사용하는 예외.
 *
 * 도메인 저장소 또는 도메인 조회 정책이 엔티티 부재를 도메인 오류로 표현할 때 던진다.
 *
 * @param code 찾을 수 없는 도메인 엔티티를 식별하는 예외 코드.
 * @param message 엔티티 부재 상황을 설명하는 메시지.
 */
class DomainEntityNotFoundException(
    code: String,
    message: String
) : ExceptionBase(code, message)
