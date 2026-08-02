package com.devneopark.chat.restapi.context.iam.application.port.inbound

interface RegisterUserUseCase {

    suspend fun register(command: Command): Result

    data class Command(

        val principal: String,

        val rawPassword: String,

        val displayName: String

    )

    data class Result(

        val userId: String

    )

}