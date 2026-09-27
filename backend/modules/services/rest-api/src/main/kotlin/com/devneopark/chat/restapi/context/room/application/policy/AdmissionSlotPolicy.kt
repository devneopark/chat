package com.devneopark.chat.restapi.context.room.application.policy

import com.devneopark.chat.restapi.context.room.application.exception.InvalidCapacityException

class AdmissionSlotPolicy(

    private val maxCapacity: Int

) {

    fun validateCapacity(capacity: Int) {
        if (capacity !in 2..maxCapacity) {
            throw InvalidCapacityException()
        }
    }

}