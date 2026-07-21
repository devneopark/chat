package com.devneopark.chat.libs.shared.application.exception

private const val CODE = "2-000-002"

private const val MESSAGE = "Temporary unavailable."

/**
 * 일시적인 외부 의존성 또는 응용 인프라 문제로 유스케이스를 처리할 수 없음을 나타내는 예외.
 *
 * 호출자가 재시도할 수 있는지는 원인과 소비 모듈의 정책에 따라 판단해야 한다.
 * 따라서 모든 응용 실패나 영속성 예외를 이 타입으로 번역해서는 안 된다.
 *
 * @param cause 일시적 불가용 상태를 유발한 원인 예외.
 */
class ApplicationUnavailableException(

    cause: Throwable? = null,

) : ApplicationFailureException(CODE, MESSAGE, cause)
