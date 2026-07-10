package com.devneopark.chat.lib.domain.participant.model

import com.devneopark.chat.lib.domain.participant.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParticipantIdTest {

    @Test
    fun `given room id and user id when participant id is created then values are preserved`() {
        // given
        val roomId = "room-1"
        val userId = "user-1"

        // when
        val id = Participant.Id(roomId, userId)

        // then
        assertEquals(roomId, id.roomId)
        assertEquals(userId, id.userId)
    }

    @Test
    fun `given room id and user id when participant id is created from factory then values are preserved`() {
        // given
        val roomId = "room-1"
        val userId = "user-1"

        // when
        val id: ParticipantId = Participant.Id.from(roomId, userId)

        // then
        assertEquals(roomId, id.roomId)
        assertEquals(userId, id.userId)
    }

    @Test
    fun `given blank room id when participant id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.ROOM_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Participant.Id(" ", "user-1")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

    @Test
    fun `given blank user id when participant id is created then domain rule violation exception is thrown`() {
        // given
        val exceptionDefinition = ExceptionDefinition.USER_ID_REQUIRED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            Participant.Id("room-1", " ")
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
    }

}
