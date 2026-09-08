package com.devneopark.chat.restapi.context.iam.application.port.inbound

/**
 * 인증된 access credential의 JTI를 기준으로 인증정보를 폐기하는 계약이다.
 *
 * 삭제는 멱등적으로 수행하며, 이미 삭제된 인증정보가 있어도 같은 결과로 처리한다.
 */
interface RevokeAuthenticationUseCase {

    /** access credential의 JTI에 해당하는 AuthenticationGrant를 삭제한다. */
    suspend fun revoke(command: Command)

    /** 폐기할 access credential의 JTI이다. */
    data class Command(

        val jti: String

    )

}
