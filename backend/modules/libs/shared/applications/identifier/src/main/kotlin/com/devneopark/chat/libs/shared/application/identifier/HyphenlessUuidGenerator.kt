package com.devneopark.chat.libs.shared.application.identifier

import java.util.UUID

class HyphenlessUuidGenerator: IdGenerator {

    private val regex = Regex.fromLiteral("-")

    private val replacement = ""

    override fun generate(): String {
        return UUID.randomUUID()
            .toString()
            .replace(regex, replacement)
    }

}