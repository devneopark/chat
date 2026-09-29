package com.devneopark.chat.restapi.context.iam.application.port.outbound

/** raw password와 저장된 비밀번호 해시의 일치 여부를 검증하는 인프라 계약이다. */
interface PasswordVerifier {

    /** raw password가 저장된 해시와 일치하는지 확인한다. */
    suspend fun matches(rawPassword: String, hash: String): Boolean

}
