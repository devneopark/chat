package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.room.model.Room
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RoomEntityTest {

    @Test
    fun `Room을 RoomEntity로 변환한다`() {
        // given
        val room = Room(
            Room.Id("room-001"),
            "Room title",
            "hashed-password"
        )

        // when
        val entity = RoomEntity.from(room)

        // then
        assertEquals("room-001", entity.id)
        assertEquals("Room title", entity.title)
        assertEquals("hashed-password", entity.passwordHash)
    }

    @Test
    fun `RoomEntity를 Room으로 변환한다`() {
        // given
        val entity = RoomEntity().apply {
            id = "room-001"
            title = "Room title"
            passwordHash = "hashed-password"
        }

        // when
        val room = entity.toDomain()

        // then
        assertEquals("room-001", room.id.value)
        assertEquals("Room title", room.title)
        assertEquals("hashed-password", room.passwordHash)
    }

    @Test
    fun `비밀번호가 없는 Room을 RoomEntity로 변환한다`() {
        // given
        val room = Room(Room.Id("room-001"), "Open room", null)

        // when
        val entity = RoomEntity.from(room)

        // then
        assertEquals(null, entity.passwordHash)
    }

}
