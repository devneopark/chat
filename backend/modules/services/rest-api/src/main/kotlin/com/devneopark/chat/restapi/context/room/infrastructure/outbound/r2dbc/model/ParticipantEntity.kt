package com.devneopark.chat.restapi.context.room.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.participant.model.Participant
import com.devneopark.chat.lib.domain.participant.model.ParticipantRole
import com.devneopark.chat.lib.domain.room.model.Room
import com.devneopark.chat.lib.domain.user.model.User
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Table("participant")
class ParticipantEntity {

    @Id
    lateinit var id: String

    lateinit var roomId: String

    lateinit var userId: String

    @Column("participant_role")
    lateinit var role: String

    lateinit var joinedAt: Instant

    companion object {

        fun from(participant: Participant): ParticipantEntity {
            return ParticipantEntity().apply {
                id = participant.id.value
                roomId = participant.roomId.value
                userId = participant.userId.value
                role = participant.role.name
                joinedAt = participant.joinedAt.toJavaInstant()
            }
        }

    }

    fun toDomain(): Participant {
        return Participant(
            Participant.Id.from(id),
            Room.Id.from(roomId),
            User.Id.from(userId),
            ParticipantRole.valueOf(role),
            joinedAt.toKotlinInstant()
        )
    }

}
