package com.devneopark.chat.restapi.context.user.application.port.outbound

/** raw password를 저장 가능한 해시로 변환하는 인프라 계약이다. */
interface PasswordHasher {

    /** raw password를 영속화하지 않고 해시한다. */
    suspend fun hash(rawPassword: String): String

}
