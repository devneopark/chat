package com.devneopark.chat.restapi.context.user.application.port.inbound

interface UpdateUserProfileUseCase {

    suspend fun update(command: Command)

    data class Command(

        val userId: String,

        val displayName: String

    )

}