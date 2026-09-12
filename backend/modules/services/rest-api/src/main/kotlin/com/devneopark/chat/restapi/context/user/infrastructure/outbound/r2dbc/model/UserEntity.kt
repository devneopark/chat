package com.devneopark.chat.restapi.context.user.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

/** `users` 테이블의 활성 사용자 정보를 표현하는 R2DBC 엔티티다. */
@Table(name = "users")
class UserEntity {

    @Id
    lateinit var id: String

    lateinit var principal: String

    lateinit var passwordHash: String

    lateinit var displayName: String

    companion object {

        /** User 도메인 객체를 영속성 엔티티로 변환한다. */
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

    /** 영속성 엔티티를 User 도메인 객체로 변환한다. */
    fun toDomain(): User {
        val userId = User.Id.from(this.id)
        val credential = Credential(this.principal, this.passwordHash)
        val profile = Profile(this.displayName)
        return User(userId, credential, profile)
    }

}
