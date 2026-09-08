package com.devneopark.chat.restapi.context.iam.application.port.outbound

/** raw password를 저장 가능한 해시와 비교하는 인프라 계약이다. */
interface PasswordHasher {

    /** raw password를 영속화하지 않고 해시한다. */
    suspend fun hash(rawPassword: String): String

    /** raw password가 저장된 해시와 일치하는지 확인한다. */
    suspend fun matches(rawPassword: String, hash: String): Boolean

}
