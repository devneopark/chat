package com.devneopark.chat.restapi.context.room.application.exception

private const val CODE = "2-003-004"
private const val MESSAGE = "Capacity reduction is currently unavailable."

/** 정원 감소에 필요한 빈 슬롯을 확보하지 못했음을 나타낸다. */
class CapacityReductionUnavailableException(

    override val cause: Throwable? = null

) : RoomContextException(CODE, MESSAGE, cause)
