package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import org.springframework.data.relational.core.mapping.Table

@Table(name = "users")
class UserEntity {

    lateinit var id: String

    lateinit var principal: String

    lateinit var passwordHash: String

    lateinit var displayName: String

    fun toDomain(): User {
        val userId = User.Id.from(this.id)
        val credential = Credential(this.principal, this.passwordHash)
        val profile = Profile(this.displayName)
        return User(userId, credential, profile)
    }

}
