package com.devneopark.chat.restapi.context.iam.application.port.inbound

interface RevokeAuthenticationUseCase {

    suspend fun revoke(command: Command)

    data class Command(

        val jti: String

    )

}
