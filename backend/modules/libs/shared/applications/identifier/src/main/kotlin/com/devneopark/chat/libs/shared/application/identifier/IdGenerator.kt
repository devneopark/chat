package com.devneopark.chat.libs.shared.application.identifier

interface IdGenerator {

    suspend fun generate(): String

}