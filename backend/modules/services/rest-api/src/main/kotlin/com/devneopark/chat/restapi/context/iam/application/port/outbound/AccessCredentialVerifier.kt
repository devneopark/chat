package com.devneopark.chat.restapi.context.iam.application.port.outbound

interface AccessCredentialVerifier {

    suspend fun verify(serializedCredential: String): VerifiedCredential

    data class VerifiedCredential(

        val userId: String,

        val jti: String

    )

}
