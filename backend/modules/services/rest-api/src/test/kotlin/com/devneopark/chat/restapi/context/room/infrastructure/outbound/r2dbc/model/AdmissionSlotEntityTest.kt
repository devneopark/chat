package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.admission_slot.model.AdmissionSlot
import com.devneopark.chat.lib.domain.participant.model.Participant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdmissionSlotEntityTest {

    @Test
    fun `점유된 AdmissionSlot을 AdmissionSlotEntity로 변환한다`() {
        // given
        val admissionSlot = AdmissionSlot(
            AdmissionSlot.Id("room-001", 1),
            Participant.Id("participant-001")
        )

        // when
        val entity = AdmissionSlotEntity.from(admissionSlot)

        // then
        assertEquals("room-001", entity.roomId)
        assertEquals(1, entity.number)
        assertEquals("participant-001", entity.occupantParticipantId)
    }

    @Test
    fun `비점유 AdmissionSlot을 AdmissionSlotEntity로 변환한다`() {
        // given
        val admissionSlot = AdmissionSlot(
            AdmissionSlot.Id("room-001", 1),
            null
        )

        // when
        val entity = AdmissionSlotEntity.from(admissionSlot)

        // then
        assertEquals("room-001", entity.roomId)
        assertEquals(1, entity.number)
        assertNull(entity.occupantParticipantId)
    }

    @Test
    fun `비어 있는 AdmissionSlotEntity를 AdmissionSlot으로 변환한다`() {
        // given
        val entity = AdmissionSlotEntity().apply {
            roomId = "room-001"
            number = 2
            occupantParticipantId = null
        }

        // when
        val admissionSlot = entity.toDomain()

        // then
        assertEquals("room-001", admissionSlot.id.roomId)
        assertEquals(2, admissionSlot.id.number)
        assertNull(admissionSlot.occupant)
    }

    @Test
    fun `점유된 AdmissionSlotEntity를 AdmissionSlot으로 변환한다`() {
        // given
        val entity = AdmissionSlotEntity().apply {
            roomId = "room-001"
            number = 1
            occupantParticipantId = "participant-001"
        }

        // when
        val admissionSlot = entity.toDomain()

        // then
        assertEquals("room-001", admissionSlot.id.roomId)
        assertEquals(1, admissionSlot.id.number)
        assertEquals("participant-001", admissionSlot.occupant?.value)
    }

}
