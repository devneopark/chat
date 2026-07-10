package com.devneopark.chat.lib.domain.admission_slot.model

import com.devneopark.chat.lib.domain.admission_slot.reference.ExceptionDefinition
import com.devneopark.chat.lib.domain.participant.reference.ParticipantId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class AdmissionSlotTest {

    @Test
    fun `given id when admission slot is created then id is preserved and occupant is null`() {
        // given
        val id = AdmissionSlot.Id("room-1", 1)

        // when
        val admissionSlot = AdmissionSlot(id)

        // then
        assertSame(id, admissionSlot.id)
        assertNull(admissionSlot.occupant)
    }

    @Test
    fun `given occupant when admission slot is created then occupant is preserved`() {
        // given
        val occupant = TestParticipantId("room-1", "user-1")

        // when
        val admissionSlot = AdmissionSlot(AdmissionSlot.Id("room-1", 1), occupant)

        // then
        assertSame(occupant, admissionSlot.occupant)
    }

    @Test
    fun `given empty slot when occupant is assigned then occupant is updated`() {
        // given
        val admissionSlot = AdmissionSlot(AdmissionSlot.Id("room-1", 1))
        val occupant = TestParticipantId("room-1", "user-1")

        // when
        admissionSlot.assign(occupant)

        // then
        assertSame(occupant, admissionSlot.occupant)
    }

    @Test
    fun `given occupied slot when occupant is assigned then domain rule violation exception is thrown and original occupant is retained`() {
        // given
        val originalOccupant = TestParticipantId("room-1", "user-1")
        val admissionSlot = AdmissionSlot(AdmissionSlot.Id("room-1", 1), originalOccupant)
        val exceptionDefinition = ExceptionDefinition.ALREADY_OCCUPIED

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            admissionSlot.assign(TestParticipantId("room-1", "user-2"))
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertSame(originalOccupant, admissionSlot.occupant)
    }

    @Test
    fun `given occupied slot when matching occupant is revoked then occupant is removed`() {
        // given
        val occupant = TestParticipantId("room-1", "user-1")
        val admissionSlot = AdmissionSlot(AdmissionSlot.Id("room-1", 1), occupant)

        // when
        admissionSlot.revoke(TestParticipantId("room-1", "user-1"))

        // then
        assertNull(admissionSlot.occupant)
    }

    @Test
    fun `given empty slot when occupant is revoked then occupant remains null`() {
        // given
        val admissionSlot = AdmissionSlot(AdmissionSlot.Id("room-1", 1))

        // when
        admissionSlot.revoke(TestParticipantId("room-1", "user-1"))

        // then
        assertNull(admissionSlot.occupant)
    }

    @Test
    fun `given occupied slot when different occupant is revoked then domain rule violation exception is thrown and original occupant is retained`() {
        // given
        val originalOccupant = TestParticipantId("room-1", "user-1")
        val admissionSlot = AdmissionSlot(AdmissionSlot.Id("room-1", 1), originalOccupant)
        val exceptionDefinition = ExceptionDefinition.OCCUPANT_MISMATCH

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            admissionSlot.revoke(TestParticipantId("room-1", "user-2"))
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertSame(originalOccupant, admissionSlot.occupant)
    }

    private data class TestParticipantId(
        override val roomId: String,
        override val userId: String
    ) : ParticipantId

}
