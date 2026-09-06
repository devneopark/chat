package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.authentication_grant.model.AccessCredential
import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.authentication_grant.model.RenewalCredential
import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.service.UserCredentialValidator
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.iam.application.exception.WrongPasswordException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.GrantAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationCredentialManager
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.iam.application.port.outbound.UserRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinInstant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition as UserExceptionDefinition

@ExtendWith(MockitoExtension::class)
class GrantAuthenticationServiceTest {

    @Mock
    lateinit var userCredentialValidator: UserCredentialValidator

    @Mock
    lateinit var userRepositoryPort: UserRepositoryPort

    @Mock
    lateinit var passwordHasher: PasswordHasher

    @Mock
    lateinit var clock: Clock

    @Mock
    lateinit var authenticationCredentialManager: AuthenticationCredentialManager

    @Mock
    lateinit var idGenerator: IdGenerator

    @Mock
    lateinit var authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort

    @InjectMocks
    lateinit var grantAuthenticationService: GrantAuthenticationService

    @Test
    fun `인증에 성공하면 credential을 발급하고 grant를 저장한 뒤 결과를 반환한다`() = runTest {
        // given
        val command = GrantAuthenticationUseCase.Command(
            "principal",
            "RawP@ssword123"
        )
        val user = User(
            User.Id("user-001"),
            Credential(command.principal, "hashed-password"),
            Profile("Neo")
        )
        val issuedAt = Instant.parse("2026-08-11T00:00:00Z")
        val now = issuedAt.toKotlinInstant()
        val accessExpiresAt = now + 15.minutes
        val renewalExpiresAt = now + 7.days
        val grantId = AuthenticationGrant.Id("grant-001")
        val authenticationGrant = AuthenticationGrant(
            grantId,
            user.id,
            now,
            AccessCredential(
                AccessCredential.Id("access-jti-001"),
                now,
                accessExpiresAt
            ),
            RenewalCredential(
                RenewalCredential.Id("renewal-id-001"),
                now,
                renewalExpiresAt
            )
        )
        val credentialSet = AuthenticationCredentialManager.CredentialSet(
            authenticationGrant,
            "access-token",
            "renewal-id-001",
            accessExpiresAt,
            renewalExpiresAt
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(command.principal)
        willDoNothing()
            .given(userCredentialValidator)
            .validatePassword(command.rawPassword)
        given(userRepositoryPort.findByPrincipal(command.principal))
            .willReturn(user)
        given(passwordHasher.matches(command.rawPassword, user.credential.passwordHash))
            .willReturn(true)
        given(clock.instant())
            .willReturn(issuedAt)
        given(idGenerator.generate())
            .willReturn("grant-001")
        given(
            authenticationCredentialManager.issue(
                argThat<AuthenticationGrant.Id> { it.value == grantId.value } ?: grantId,
                eq(user.id) ?: user.id,
                eq(now) ?: now
            )
        )
            .willReturn(credentialSet)

        // when
        val result = grantAuthenticationService.grant(command)

        // then
        assertEquals("user-001", result.userId)
        assertEquals("access-token", result.accessCredential.serializedValue)
        assertEquals(accessExpiresAt, result.accessCredential.expiresAt)
        assertEquals("renewal-id-001", result.renewalCredential.serializedValue)
        assertEquals(renewalExpiresAt, result.renewalCredential.expiresAt)

        verify(authenticationGrantRepositoryPort, only())
            .insert(authenticationGrant)
    }

    @Test
    fun `principal 입력값 검사를 통과하지 못하면 이후 인증 절차를 수행하지 않는다`() = runTest {
        // given
        val command = GrantAuthenticationUseCase.Command(
            "pr!nc!pa/",
            "RawP@ssword123"
        )
        given(userCredentialValidator.validatePrincipal(command.principal))
            .willThrow(
                DomainRuleViolationException(
                    UserExceptionDefinition.INVALID_USER_PRINCIPAL.code,
                    UserExceptionDefinition.INVALID_USER_PRINCIPAL.message
                )
            )

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            grantAuthenticationService.grant(command)
        }

        // then
        assertEquals(UserExceptionDefinition.INVALID_USER_PRINCIPAL.code, exception.code)
        assertEquals(UserExceptionDefinition.INVALID_USER_PRINCIPAL.message, exception.message)
        verify(userCredentialValidator, only())
            .validatePrincipal(command.principal)
        verifyNoInteractions(
            userRepositoryPort,
            passwordHasher,
            clock,
            authenticationCredentialManager,
            idGenerator,
            authenticationGrantRepositoryPort
        )
    }

    @Test
    fun `password 입력값 검사를 통과하지 못하면 이후 인증 절차를 수행하지 않는다`() = runTest {
        // given
        val command = GrantAuthenticationUseCase.Command(
            "principal",
            "password"
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(command.principal)
        willThrow(
            DomainRuleViolationException(
                UserExceptionDefinition.INVALID_USER_PASSWORD.code,
                UserExceptionDefinition.INVALID_USER_PASSWORD.message
            )
        ).given(userCredentialValidator)
            .validatePassword(command.rawPassword)

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            grantAuthenticationService.grant(command)
        }

        // then
        assertEquals(UserExceptionDefinition.INVALID_USER_PASSWORD.code, exception.code)
        assertEquals(UserExceptionDefinition.INVALID_USER_PASSWORD.message, exception.message)
        verify(userCredentialValidator)
            .validatePassword(command.rawPassword)
        verifyNoInteractions(
            userRepositoryPort,
            passwordHasher,
            clock,
            authenticationCredentialManager,
            idGenerator,
            authenticationGrantRepositoryPort
        )
    }

    @Test
    fun `사용자를 찾지 못하면 UserNotFoundException을 던지고 credential을 발급하지 않는다`() = runTest {
        // given
        val command = GrantAuthenticationUseCase.Command(
            "principal",
            "RawP@ssword123"
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(command.principal)
        willDoNothing()
            .given(userCredentialValidator)
            .validatePassword(command.rawPassword)
        given(userRepositoryPort.findByPrincipal(command.principal))
            .willReturn(null)

        // when
        val exception = assertFailsWith<UserNotFoundException> {
            grantAuthenticationService.grant(command)
        }

        // then
        assertEquals("2-001-002", exception.code)
        assertEquals("User not found.", exception.message)
        verify(userRepositoryPort, only())
            .findByPrincipal(command.principal)
        verifyNoInteractions(
            passwordHasher,
            clock,
            authenticationCredentialManager,
            idGenerator,
            authenticationGrantRepositoryPort
        )
    }

    @Test
    fun `비밀번호가 일치하지 않으면 WrongPasswordException을 던지고 credential을 발급하지 않는다`() = runTest {
        // given
        val command = GrantAuthenticationUseCase.Command(
            "principal",
            "RawP@ssword123"
        )
        val user = User(
            User.Id("user-001"),
            Credential(command.principal, "hashed-password"),
            Profile("Neo")
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(command.principal)
        willDoNothing()
            .given(userCredentialValidator)
            .validatePassword(command.rawPassword)
        given(userRepositoryPort.findByPrincipal(command.principal))
            .willReturn(user)
        given(passwordHasher.matches(command.rawPassword, user.credential.passwordHash))
            .willReturn(false)

        // when
        val exception = assertFailsWith<WrongPasswordException> {
            grantAuthenticationService.grant(command)
        }

        // then
        assertEquals("2-001-003", exception.code)
        assertEquals("Wrong password.", exception.message)
        verify(passwordHasher, only())
            .matches(command.rawPassword, user.credential.passwordHash)
        verifyNoInteractions(
            clock,
            authenticationCredentialManager,
            idGenerator,
            authenticationGrantRepositoryPort
        )
    }

}
