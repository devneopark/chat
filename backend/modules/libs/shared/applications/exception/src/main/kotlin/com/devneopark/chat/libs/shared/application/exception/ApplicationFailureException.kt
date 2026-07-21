package com.devneopark.chat.libs.shared.application.exception

import com.devneopark.chat.lib.shared.kernel.exception.ExceptionBase

/**
 * 유스케이스 처리 실패를 표현하는 응용 계층 예외의 공통 기반 타입.
 *
 * 공통 응용 경계에서 영속성 또는 외부 시스템의 기술 예외를 응용 계층이
 * 이해할 수 있는 실패 범주로 번역할 때 사용할 수 있다. 이 타입 자체는
 * 특정 도메인이나 영속 기술을 알지 않으며, 구체적인 비즈니스 의미가 필요한
 * 실패는 소비 모듈에서 별도의 예외 또는 유스케이스 결과로 표현해야 한다.
 *
 * @param code 응용 예외를 식별하는 코드.
 * @param message 응용 예외를 설명하는 메시지.
 * @param cause 이 예외를 유발한 원인 예외.
 */
abstract class ApplicationFailureException(

    code: String,

    message: String,

    cause: Throwable? = null

) : ExceptionBase(code, message, cause)
