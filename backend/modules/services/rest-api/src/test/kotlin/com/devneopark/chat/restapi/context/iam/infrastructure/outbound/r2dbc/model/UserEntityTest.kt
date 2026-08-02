package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UserEntityTest {

    @Test
    fun `User를 UserEntity로 변환한다`() {
        val user = User(
            User.Id("user-id"),
            Credential("dev.neopark", "hashed-password"),
            Profile("Neo")
        )

        val entity = UserEntity.from(user)

        assertEquals("user-id", entity.id)
        assertEquals("dev.neopark", entity.principal)
        assertEquals("hashed-password", entity.passwordHash)
        assertEquals("Neo", entity.displayName)
    }

    @Test
    fun `UserEntity를 User로 변환한다`() {
        val entity = UserEntity().apply {
            id = "user-id"
            principal = "dev.neopark"
            passwordHash = "hashed-password"
            displayName = "Neo"
        }

        val user = entity.toDomain()

        assertEquals("user-id", user.id.value)
        assertEquals("dev.neopark", user.credential.principal)
        assertEquals("hashed-password", user.credential.passwordHash)
        assertEquals("Neo", user.profile.displayName)
    }

}
