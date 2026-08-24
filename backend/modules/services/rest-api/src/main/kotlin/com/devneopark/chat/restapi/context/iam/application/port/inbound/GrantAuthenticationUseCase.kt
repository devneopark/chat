package com.devneopark.chat.restapi.context.iam.application.port.inbound

import kotlin.time.Instant

interface GrantAuthenticationUseCase {

    suspend fun grant(command: Command): Result

    data class Command(

        val principal: String,

        val rawPassword: String

    )

    data class Result(

        val userId: String,

        val accessCredential: Credential,

        val renewalCredential: Credential

    )

    data class Credential(

        val serializedValue: String,

        val expiresAt: Instant

    )

}