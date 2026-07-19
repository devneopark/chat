package com.devneopark.chat.lib.shared.kernel.exception

/**
 * 애플리케이션 전역에서 사용하는 비즈니스 예외의 공통 기반 타입.
 *
 * 모든 하위 예외는 추적 가능한 [code]와 사용자 또는 호출자에게 전달할 [message]를 가져야 한다.
 * 예외 객체 생성 비용과 로그 노이즈를 줄이기 위해 stack trace와 suppressed exception은 비활성화한다.
 *
 * @param code 예외를 식별하는 공통 코드. blank 값은 허용하지 않는다.
 * @param message 예외 상황을 설명하는 메시지. blank 값은 허용하지 않는다.
 * @param cause 이 예외를 유발한 원인 예외.
 * @throws IllegalArgumentException [code] 또는 [message]가 blank인 경우.
 */
abstract class ExceptionBase(

    /**
     * 예외 코드 체계
     *
     * `<소속_레이어>-<컨텍스트_번호>-<예외_케이스_번호>` 형태.
     * - 소속_레이어: 숫자 1자리
     *     - `1`: 도메인 레이어
     *     - `2`: 응용 레이어
     *     - `3`: 인프라 레이어
     * - 컨텍스트(또는 도메인)_번호: 숫자 3자리
     *     - `000`: 공용
     * - 예외_케이스_번호: 숫자 3자리
     *
     * 예시: `1-002-003`
     */
    val code: String,

    message: String,

    cause: Throwable? = null

) : RuntimeException(message, cause, false, false) {

    init {
        if (code.isBlank()) {
            throw IllegalArgumentException("exception code can't be empty")
        }
        if (message.isBlank()) {
            throw IllegalArgumentException("exception message can't be empty")
        }
    }

}
