package com.devneopark.chat.restapi.context.user.application.port.outbound

import com.devneopark.chat.lib.domain.user.reference.UserId

/** 사용자의 활성 채팅방 참여 여부를 확인하는 응용 계층 계약이다. */
fun interface ActiveRoomParticipationChecker {

    /** 사용자가 활성 채팅방에 참여 중인지 확인한다. */
    suspend fun existsByUserId(userId: UserId): Boolean

}
