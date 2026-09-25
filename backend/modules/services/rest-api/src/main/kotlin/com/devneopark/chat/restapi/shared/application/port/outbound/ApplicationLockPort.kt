package com.devneopark.chat.restapi.shared.application.port.outbound

/** 응용 계층에서 공유하는 논리적 잠금 획득 포트다. */
interface ApplicationLockPort {

    /** 주어진 키에 대한 잠금을 획득한다. */
    suspend fun lock(key: String)

}
