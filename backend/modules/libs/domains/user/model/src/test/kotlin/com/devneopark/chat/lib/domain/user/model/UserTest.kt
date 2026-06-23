package com.devneopark.chat.lib.domain.user.model

import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class UserTest {

    @Test
    fun `given id credential and profile when user is created then values are preserved`() {
        // given
        val id = User.Id("user-1")
        val credential = Credential("user@example.com", "hashed-password")
        val profile = Profile("Neo")

        // when
        val user = User(id, credential, profile)

        // then
        assertSame(id, user.id)
        assertSame(credential, user.credential)
        assertSame(profile, user.profile)
    }

    @Test
    fun `given new password hash when password is changed then password hash is updated`() {
        // given
        val user = User(
            User.Id("user-1"),
            Credential("user@example.com", "old-hashed-password"),
            Profile("Neo")
        )
        val newPasswordHash = "new-hashed-password"

        // when
        user.changePassword(newPasswordHash)

        // then
        assertEquals(newPasswordHash, user.credential.passwordHash)
    }

    @Test
    fun `given blank password hash when password is changed then exception is thrown and original password hash is retained`() {
        // given
        val originalPasswordHash = "old-hashed-password"
        val user = User(
            User.Id("user-1"),
            Credential("user@example.com", originalPasswordHash),
            Profile("Neo")
        )
        val newPasswordHash = " "
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_PASSWORD

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            user.changePassword(newPasswordHash)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalPasswordHash, user.credential.passwordHash)
    }

    @Test
    fun `given new display name when display name is changed then display name is updated`() {
        // given
        val user = User(
            User.Id("user-1"),
            Credential("user@example.com", "hashed-password"),
            Profile("Neo")
        )
        val newDisplayName = "Park"

        // when
        user.changeDisplayName(newDisplayName)

        // then
        assertEquals(newDisplayName, user.profile.displayName)
    }

    @Test
    fun `given blank display name when display name is changed then exception is thrown and original display name is retained`() {
        // given
        val originalDisplayName = "Neo"
        val user = User(
            User.Id("user-1"),
            Credential("user@example.com", "hashed-password"),
            Profile(originalDisplayName)
        )
        val newDisplayName = " "
        val exceptionDefinition = ExceptionDefinition.INVALID_USER_DISPLAY_NAME

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            user.changeDisplayName(newDisplayName)
        }

        // then
        assertEquals(exceptionDefinition.code, exception.code)
        assertEquals(exceptionDefinition.message, exception.message)
        assertEquals(originalDisplayName, user.profile.displayName)
    }

}
