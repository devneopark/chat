package com.devneopark.chat.restapi.context.user.application.service

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.user.application.exception.UserHasActiveRoomException
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.WithdrawUserUseCase
import com.devneopark.chat.restapi.context.user.application.port.outbound.ActiveRoomParticipationChecker
import com.devneopark.chat.restapi.context.user.application.port.outbound.AuthenticationGrantRevoker
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant

@ExtendWith(MockitoExtension::class)
class WithdrawUserServiceTest {

    @Mock
    lateinit var userRepositoryPort: UserRepositoryPort

    @Mock
    lateinit var activeRoomParticipationChecker: ActiveRoomParticipationChecker

    @Mock
    lateinit var authenticationGrantRevoker: AuthenticationGrantRevoker

    @Mock
    lateinit var clock: Clock

    @InjectMocks
    lateinit var withdrawUserService: WithdrawUserService

    @Test
    fun `활성 채팅방에 참여하지 않은 사용자를 탈퇴 처리하고 인증정보를 폐기한다`() = runTest {
        // given
        val userId = "user-001"
        val command = WithdrawUserUseCase.Command(userId)
        val user = User(
            User.Id(userId),
            Credential("principal", "hashed-password"),
            Profile("Neo")
        )
        val withdrawnAt = Instant.parse("2026-09-28T00:00:00Z")
        given(userRepositoryPort.findByIdForUpdate(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(user)
        given(activeRoomParticipationChecker.existsByUserId(
            argThat<User.Id> { it.value == userId } ?: User.Id(userId)
        )).willReturn(false)
        given(clock.instant()).willReturn(withdrawnAt)

        // when
        withdrawUserService.withdraw(command)

        // then
        verify(userRepositoryPort)
            .findByIdForUpdate(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
        verify(activeRoomParticipationChecker)
            .existsByUserId(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
        verify(userRepositoryPort).withdraw(
            argThat<User.Id> { it.value == userId } ?: User.Id(userId),
            argThat<kotlin.time.Instant> { it == withdrawnAt.toKotlinInstant() }
                ?: withdrawnAt.toKotlinInstant()
        )
        verify(clock).instant()
        verify(authenticationGrantRevoker)
            .revokeAllByUserId(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
    }

    @Test
    fun `사용자가 존재하지 않으면 예외를 던지고 탈퇴 처리하지 않는다`() = runTest {
        // given
        val userId = "user-001"
        val command = WithdrawUserUseCase.Command(userId)
        given(userRepositoryPort.findByIdForUpdate(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(null)

        // when
        val exception = assertFailsWith<UserNotFoundException> {
            withdrawUserService.withdraw(command)
        }

        // then
        assertEquals("2-002-001", exception.code)
        verify(userRepositoryPort, only())
            .findByIdForUpdate(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
        verifyNoInteractions(
            activeRoomParticipationChecker,
            authenticationGrantRevoker,
            clock
        )
    }

    @Test
    fun `활성 채팅방에 참여 중이면 예외를 던지고 탈퇴 처리하지 않는다`() = runTest {
        // given
        val userId = "user-001"
        val command = WithdrawUserUseCase.Command(userId)
        val user = User(
            User.Id(userId),
            Credential("principal", "hashed-password"),
            Profile("Neo")
        )
        given(userRepositoryPort.findByIdForUpdate(argThat<User.Id> { it.value == userId } ?: User.Id(userId)))
            .willReturn(user)
        given(activeRoomParticipationChecker.existsByUserId(
            argThat<User.Id> { it.value == userId } ?: User.Id(userId)
        )).willReturn(true)

        // when
        val exception = assertFailsWith<UserHasActiveRoomException> {
            withdrawUserService.withdraw(command)
        }

        // then
        assertEquals("2-002-002", exception.code)
        verify(userRepositoryPort)
            .findByIdForUpdate(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
        verify(activeRoomParticipationChecker)
            .existsByUserId(argThat<User.Id> { it.value == userId } ?: User.Id(userId))
        verify(userRepositoryPort, never()).withdraw(
            argThat<User.Id> { it.value == userId } ?: User.Id(userId),
            argThat<kotlin.time.Instant> { true } ?: Instant.parse("2026-09-28T00:00:00Z").toKotlinInstant()
        )
        verifyNoInteractions(authenticationGrantRevoker, clock)
    }

}
