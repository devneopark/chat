package com.devneopark.chat.lib.domain.admission_slot.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class AdmissionSlotIdTest {

    @Test
    fun `given admission slot id implementation when values are read then original values are exposed`() {
        // given
        val roomId = "room-1"
        val number = 1
        val admissionSlotId: AdmissionSlotId = TestAdmissionSlotId(roomId, number)

        // when
        val actualRoomId = admissionSlotId.roomId
        val actualNumber = admissionSlotId.number

        // then
        assertEquals(roomId, actualRoomId)
        assertEquals(number, actualNumber)
    }

    private data class TestAdmissionSlotId(
        override val roomId: String,
        override val number: Int
    ) : AdmissionSlotId

}
