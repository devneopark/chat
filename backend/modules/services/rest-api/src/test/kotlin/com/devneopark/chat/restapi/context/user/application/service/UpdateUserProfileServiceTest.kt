package com.devneopark.chat.restapi.context.user.application.service

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.UpdateUserProfileUseCase
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class UpdateUserProfileServiceTest {

    @Mock
    lateinit var userRepositoryPort: UserRepositoryPort

    @InjectMocks
    lateinit var updateUserProfileService: UpdateUserProfileService

    @Test
    fun `사용자의 displayName을 변경하고 저장한다`() = runTest {
        // given
        val userId = "user-001"
        val displayName = "Updated Neo"
        val command = UpdateUserProfileUseCase.Command(userId, displayName)
        val user = User(
            User.Id(userId),
            Credential("principal", "hashed-password"),
            Profile("Neo")
        )
        given(userRepositoryPort.findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(user)

        // when
        updateUserProfileService.update(command)

        // then
        assertEquals(displayName, user.profile.displayName)
        verify(userRepositoryPort)
            .findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
        verify(userRepositoryPort).updateProfile(user)
    }

    @Test
    fun `사용자가 존재하지 않으면 예외를 던지고 저장하지 않는다`() = runTest {
        // given
        val userId = "user-001"
        val command = UpdateUserProfileUseCase.Command(userId, "Updated Neo")
        given(userRepositoryPort.findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(null)

        // when
        val exception = assertFailsWith<UserNotFoundException> {
            updateUserProfileService.update(command)
        }

        // then
        assertEquals("2-002-001", exception.code)
        assertEquals("User not found.", exception.message)
        verify(userRepositoryPort, only())
            .findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
    }

    @Test
    fun `displayName 변경 중 DomainRuleViolationException이 발생하면 저장하지 않는다`() = runTest {
        // given
        val userId = "user-001"
        val command = UpdateUserProfileUseCase.Command(userId, "")
        val user = User(
            User.Id(userId),
            Credential("principal", "hashed-password"),
            Profile("Neo")
        )
        given(userRepositoryPort.findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(user)

        // when
        assertFailsWith<DomainRuleViolationException> {
            updateUserProfileService.update(command)
        }

        // then
        verify(userRepositoryPort, only())
            .findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
    }

    @Test
    fun `displayName이 기존 값과 같으면 저장하지 않는다`() = runTest {
        // given
        val userId = "user-001"
        val displayName = "Neo"
        val command = UpdateUserProfileUseCase.Command(userId, displayName)
        val user = User(
            User.Id(userId),
            Credential("principal", "hashed-password"),
            Profile(displayName)
        )
        given(userRepositoryPort.findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(user)

        // when
        updateUserProfileService.update(command)

        // then
        verify(userRepositoryPort, only())
            .findById(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
    }

}
