package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.authentication_grant.model.AccessCredential
import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.authentication_grant.model.RenewalCredential
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.ExceptionDefinition
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RenewalAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth.NimbusAuthenticationCredentialManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@ExtendWith(MockitoExtension::class)
class RenewalAuthenticationServiceTest {

    @Mock
    lateinit var authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort

    @Mock
    lateinit var clock: Clock

    @Mock
    lateinit var authenticationCredentialManager: NimbusAuthenticationCredentialManager

    @Mock
    lateinit var idGenerator: IdGenerator

    @InjectMocks
    lateinit var renewalAuthenticationService: RenewalAuthenticationService

    @Test
    fun `존재하지 않는 renewal credential이면 INVALID_RENEWAL_CREDENTIAL 예외를 던진다`() = runTest {
        // given
        val command = RenewalAuthenticationUseCase.Command("missing-renewal-id")
        given(authenticationGrantRepositoryPort.findByRenewalCredentialId(command.renewalCredentialId))
            .willReturn(null)

        // when
        val exception = assertFailsWith<IamContextException> {
            renewalAuthenticationService.renewal(command)
        }

        // then
        assertEquals(ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.message, exception.message)
        verify(authenticationGrantRepositoryPort, only())
            .findByRenewalCredentialId(command.renewalCredentialId)
        verifyNoInteractions(clock, authenticationCredentialManager, idGenerator)
    }

    @Test
    fun `만료된 renewal credential이면 INVALID_RENEWAL_CREDENTIAL 예외를 던진다`() = runTest {
        // given
        val issuedAt = Instant.parse("2026-08-11T00:00:00Z")
        val renewalExpiresAt = issuedAt + 1.minutes
        val authenticationGrant = AuthenticationGrant(
            AuthenticationGrant.Id("grant-001"),
            User.Id("user-001"),
            issuedAt,
            AccessCredential(
                AccessCredential.Id("access-jti-001"),
                issuedAt,
                issuedAt + 15.minutes
            ),
            RenewalCredential(
                RenewalCredential.Id("renewal-id-001"),
                issuedAt,
                renewalExpiresAt
            )
        )
        val command = RenewalAuthenticationUseCase.Command("renewal-id-001")
        given(authenticationGrantRepositoryPort.findByRenewalCredentialId(command.renewalCredentialId))
            .willReturn(authenticationGrant)
        given(clock.instant())
            .willReturn(renewalExpiresAt.toJavaInstant())

        // when
        val exception = assertFailsWith<IamContextException> {
            renewalAuthenticationService.renewal(command)
        }

        // then
        assertEquals(ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.message, exception.message)
        verify(authenticationGrantRepositoryPort, only())
            .findByRenewalCredentialId(command.renewalCredentialId)
        verify(clock, only())
            .instant()
        verifyNoInteractions(authenticationCredentialManager, idGenerator)
    }

}
