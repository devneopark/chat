package com.devneopark.chat.lib.domain.room.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class RoomIdTest {

    @Test
    fun `given room id implementation when value is read then original value is exposed`() {
        // given
        val value = "room-1"
        val roomId: RoomId = TestRoomId(value)

        // when
        val actualValue = roomId.value

        // then
        assertEquals(value, actualValue)
    }

    private data class TestRoomId(
        override val value: String
    ) : RoomId

}
