package com.devneopark.chat.lib.domain.admission_slot.model

import com.devneopark.chat.lib.domain.admission_slot.reference.AdmissionSlotId
import com.devneopark.chat.lib.domain.admission_slot.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdmissionSlotIdTest {

    @Test
    fun `given room id and number when admission slot id is created then values are preserved`() {
        // given
        val roomId = "room-1"
        val number = 1

        // when
        val id = AdmissionSlot.Id(roomId, number)

        // then
        assertEquals(roomId, id.roomId)
        assertEquals(number, id.number)
    }

    @Test
    fun `given room id and number when admission slot id is created from factory then values are preserved`() {
        // given
        val roomId = "room-1"
        val number = 1

        // when
        val id: AdmissionSlotId = AdmissionSlot.Id.from(roomId, number)

        // then
        assertEquals(roomId, id.roomId)
        assertEquals(number, id.number)
    }

    @Test
    fun `given blank room id when admission slot id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.ROOM_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            AdmissionSlot.Id(" ", 1)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given zero number when admission slot id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.INVALID_SLOT_NUMBER

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            AdmissionSlot.Id("room-1", 0)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
