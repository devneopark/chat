package com.devneopark.chat.libs.shared.application.identifier

import java.util.UUID

/**
 * 하이픈 없는 UUID 문자열을 생성하는 [IdGenerator] 구현체.
 *
 * 32자의 소문자 16진수 문자열 반환.
 */
class HyphenlessUuidGenerator: IdGenerator {

    private val regex = Regex.fromLiteral("-")

    private val replacement = ""

    override suspend fun generate(): String {
        return UUID.randomUUID()
            .toString()
            .replace(regex, replacement)
    }

}
