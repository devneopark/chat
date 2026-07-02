package com.devneopark.chat.lib.domain.room.model

import com.devneopark.chat.lib.domain.room.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class RoomTest {

    @Test
    fun `given metadata when room is created then values are preserved`() {
        // given
        val id = Room.Id("room-1")
        val hostUserId = Room.HostId("user-1")
        val title = "Room title"
        val passwordHash = "hashed-password"

        // when
        val room = Room(id, hostUserId, title, passwordHash)

        // then
        assertSame(id, room.id)
        assertSame(hostUserId, room.hostUserId)
        assertEquals(title, room.title)
        assertEquals(passwordHash, room.passwordHash)
    }

    @Test
    fun `given no password hash when room is created then password hash is null`() {
        // when
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), "Room title", null)

        // then
        assertNull(room.passwordHash)
    }

    @Test
    fun `given max length title when room is created then title is preserved`() {
        // given
        val title = "a".repeat(50)

        // when
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), title, null)

        // then
        assertEquals(title, room.title)
    }

    @Test
    fun `given blank title when room is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Room(Room.Id("room-1"), Room.HostId("user-1"), " ", null)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given too long title when room is created then domain rule violation exception is thrown`() {
        // given
        val title = "a".repeat(51)
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Room(Room.Id("room-1"), Room.HostId("user-1"), title, null)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given blank password hash when room is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Room(Room.Id("room-1"), Room.HostId("user-1"), "Room title", " ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given new host when host is changed then host is updated`() {
        // given
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), "Room title", null)
        val newHostId = Room.HostId("user-2")

        // when
        room.changeHost(newHostId)

        // then
        assertSame(newHostId, room.hostUserId)
    }

    @Test
    fun `given new title when title is changed then title is updated`() {
        // given
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), "Original title", null)
        val newTitle = "New title"

        // when
        room.changeTitle(newTitle)

        // then
        assertEquals(newTitle, room.title)
    }

    @Test
    fun `given invalid title when title is changed then exception is thrown and original title is retained`() {
        // given
        val originalTitle = "Original title"
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), originalTitle, null)
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_TITLE

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            room.changeTitle(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalTitle, room.title)
    }

    @Test
    fun `given new password hash when password hash is changed then password hash is updated`() {
        // given
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), "Room title", "old-hash")
        val newPasswordHash = "new-hash"

        // when
        room.changePasswordHash(newPasswordHash)

        // then
        assertEquals(newPasswordHash, room.passwordHash)
    }

    @Test
    fun `given null when password hash is changed then password is removed`() {
        // given
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), "Room title", "hashed-password")

        // when
        room.changePasswordHash(null)

        // then
        assertNull(room.passwordHash)
    }

    @Test
    fun `given blank password hash when password hash is changed then exception is thrown and original hash is retained`() {
        // given
        val originalPasswordHash = "original-hash"
        val room = Room(Room.Id("room-1"), Room.HostId("user-1"), "Room title", originalPasswordHash)
        val exceptionDefinition = ExceptionDefinition.INVALID_ROOM_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            room.changePasswordHash(" ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalPasswordHash, room.passwordHash)
    }

}
