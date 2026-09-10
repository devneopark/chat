package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "users")
class UserEntity {

    @Id
    lateinit var id: String

    lateinit var principal: String

    lateinit var passwordHash: String

    lateinit var displayName: String

    companion object {

        fun from(user: User): UserEntity {
            val entity = UserEntity().apply {
                id = user.id.value
                principal = user.credential.principal
                passwordHash = user.credential.passwordHash
                displayName = user.profile.displayName
            }
            return entity
        }

    }

    fun toDomain(): User {
        val userId = User.Id.from(this.id)
        val credential = Credential(this.principal, this.passwordHash)
        val profile = Profile(this.displayName)
        return User(userId, credential, profile)
    }

}
