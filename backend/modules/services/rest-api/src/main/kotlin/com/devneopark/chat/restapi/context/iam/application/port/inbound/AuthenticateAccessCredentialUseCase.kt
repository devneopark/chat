package com.devneopark.chat.restapi.context.iam.application.port.inbound

interface AuthenticateAccessCredentialUseCase {

    suspend fun authenticate(command: Command): Result

    data class Command(

        val serializedCredential: String

    )

    data class Result(

        val userId: String,

        val jti: String

    )

}
