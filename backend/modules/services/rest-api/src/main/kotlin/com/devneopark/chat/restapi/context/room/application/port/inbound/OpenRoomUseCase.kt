package com.devneopark.chat.restapi.context.room.application.port.inbound

interface OpenRoomUseCase {

    suspend fun open(command: Command): Result

    data class Command(

        val title: String,

        val rawPassword: String?,

        val capacity: Int,

        val hostUserId: String

    )

    data class Result(

        val roomId: String

    )

}