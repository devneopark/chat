package com.devneopark.chat.lib.domain.room.model

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.room.reference.RoomId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomIdTest {

    @Test
    fun `given value when room id is created then value is preserved`() {
        // given
        val value = "room-1"

        // when
        val id = Room.Id(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given value when room id is created from factory then value is preserved`() {
        // given
        val value = "room-1"

        // when
        val id: RoomId = Room.Id.from(value)

        // then
        assertEquals(value, id.value)
    }

    @Test
    fun `given blank value when room id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.ROOM_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Room.Id(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
