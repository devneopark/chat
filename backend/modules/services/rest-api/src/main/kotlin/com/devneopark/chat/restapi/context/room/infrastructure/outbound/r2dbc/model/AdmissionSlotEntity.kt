package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.admission_slot.model.AdmissionSlot
import com.devneopark.chat.lib.domain.participant.model.Participant
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("admission_slot")
class AdmissionSlotEntity {

    lateinit var roomId: String

    @Column("slot_number")
    var number: Int = 0

    var occupantParticipantId: String? = null

    companion object {

        fun from(admissionSlot: AdmissionSlot): AdmissionSlotEntity {
            return AdmissionSlotEntity().apply {
                roomId = admissionSlot.id.roomId
                number = admissionSlot.id.number
                occupantParticipantId = admissionSlot.occupant?.value
            }
        }

    }

    fun toDomain(): AdmissionSlot {
        return AdmissionSlot(
            AdmissionSlot.Id.from(roomId, number),
            occupantParticipantId?.let(Participant.Id::from)
        )
    }

}
