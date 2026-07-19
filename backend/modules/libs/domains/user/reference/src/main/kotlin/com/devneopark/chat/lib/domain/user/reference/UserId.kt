package com.devneopark.chat.lib.domain.user.reference

/**
 * 사용자 도메인 경계에서 공유하는 사용자 식별자 계약.
 *
 * 사용자 모델을 직접 의존하지 않는 모듈은 이 인터페이스를 통해 사용자 식별자 값만 참조한다.
 */
interface UserId {

    /**
     * 사용자 식별자 값.
     */
    val value: String

}
