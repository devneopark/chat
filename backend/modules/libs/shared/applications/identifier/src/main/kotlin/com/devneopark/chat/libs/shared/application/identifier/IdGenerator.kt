package com.devneopark.chat.libs.shared.application.identifier

/**
 * 문자열 식별자 생성 계약.
 */
interface IdGenerator {

    /**
     * 새로운 문자열 식별자 생성.
     *
     * @return 생성된 식별자
     */
    suspend fun generate(): String

}
