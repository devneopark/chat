package com.devneopark.chat.libs.shared.application.exception

private const val CODE = "2-000-001"

private const val MESSAGE = "Conflict occurred."

/**
 * 현재 상태와 충돌하여 유스케이스를 처리할 수 없음을 나타내는 일반 응용 예외.
 *
 * 이메일 중복처럼 특정 유스케이스의 의미를 표현하기 위한 예외가 아니라,
 * 공통 경계에서 구체적인 원인을 안전하게 특정할 수 없는 충돌을 표현한다.
 * 구체적인 비즈니스 오류 코드가 필요하면 소비 모듈에서 별도 결과 또는
 * [ApplicationFailureException] 하위 타입으로 표현해야 한다.
 *
 * @param cause 충돌을 유발한 원인 예외.
 */
class ApplicationConflictException(

    cause: Throwable? = null,

) : ApplicationFailureException(CODE, MESSAGE, cause)
