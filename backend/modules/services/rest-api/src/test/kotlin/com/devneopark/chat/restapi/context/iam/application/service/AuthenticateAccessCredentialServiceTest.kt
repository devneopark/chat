package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.restapi.context.iam.application.exception.ExceptionDefinition
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AccessCredentialVerifier
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant

@ExtendWith(MockitoExtension::class)
class AuthenticateAccessCredentialServiceTest {

    @Mock
    lateinit var accessCredentialVerifier: AccessCredentialVerifier

    @Mock
    lateinit var authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort

    @Mock
    lateinit var authenticationGrant: AuthenticationGrant

    @Mock
    lateinit var clock: Clock

    @InjectMocks
    lateinit var authenticateAccessCredentialService: AuthenticateAccessCredentialService

    @Test
    fun `사용 가능한 access credential이면 인증 결과를 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val jti = "access-jti-001"
        val now = Instant.parse("2026-08-11T00:00:00Z")
        given(accessCredentialVerifier.verify(serializedCredential))
            .willReturn(AccessCredentialVerifier.VerifiedCredential("user-001", jti))
        given(authenticationGrantRepositoryPort.findByJti(jti))
            .willReturn(authenticationGrant)
        given(authenticationGrant.userId)
            .willReturn(User.Id("user-001"))
        given(clock.instant())
            .willReturn(now)
        given(authenticationGrant.isAccessCredentialUsable(now.toKotlinInstant()))
            .willReturn(true)

        // when
        val result = authenticateAccessCredentialService.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )

        // then
        assertEquals("user-001", result.userId)
        assertEquals(jti, result.jti)
    }

    @Test
    fun `access credential에 해당하는 grant가 없으면 인증을 거부한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val jti = "missing-jti"
        given(accessCredentialVerifier.verify(serializedCredential))
            .willReturn(AccessCredentialVerifier.VerifiedCredential("user-001", jti))
        given(authenticationGrantRepositoryPort.findByJti(jti))
            .willReturn(null)

        // when
        val exception = assertFailsWith<IamContextException> {
            authenticateAccessCredentialService.authenticate(
                AuthenticateAccessCredentialUseCase.Command(serializedCredential)
            )
        }

        // then
        assertEquals(ExceptionDefinition.INVALID_ACCESS_CREDENTIAL.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_ACCESS_CREDENTIAL.message, exception.message)
    }

    @Test
    fun `사용할 수 없는 access credential이면 인증을 거부한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val jti = "expired-jti"
        val now = Instant.parse("2026-08-11T00:00:00Z")
        given(accessCredentialVerifier.verify(serializedCredential))
            .willReturn(AccessCredentialVerifier.VerifiedCredential("user-001", jti))
        given(authenticationGrantRepositoryPort.findByJti(jti))
            .willReturn(authenticationGrant)
        given(authenticationGrant.userId)
            .willReturn(User.Id("user-001"))
        given(clock.instant())
            .willReturn(now)
        given(authenticationGrant.isAccessCredentialUsable(now.toKotlinInstant()))
            .willReturn(false)

        // when
        val exception = assertFailsWith<IamContextException> {
            authenticateAccessCredentialService.authenticate(
                AuthenticateAccessCredentialUseCase.Command(serializedCredential)
            )
        }

        // then
        assertEquals(ExceptionDefinition.INVALID_ACCESS_CREDENTIAL.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_ACCESS_CREDENTIAL.message, exception.message)
    }

    @Test
    fun `토큰의 userId와 grant의 userId가 다르면 인증을 거부한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val jti = "access-jti-001"
        given(accessCredentialVerifier.verify(serializedCredential))
            .willReturn(AccessCredentialVerifier.VerifiedCredential("user-from-token", jti))
        given(authenticationGrantRepositoryPort.findByJti(jti))
            .willReturn(authenticationGrant)
        given(authenticationGrant.userId)
            .willReturn(User.Id("user-from-grant"))

        // when
        val exception = assertFailsWith<IamContextException> {
            authenticateAccessCredentialService.authenticate(
                AuthenticateAccessCredentialUseCase.Command(serializedCredential)
            )
        }

        // then
        assertEquals(ExceptionDefinition.INVALID_ACCESS_CREDENTIAL.code, exception.code)
        assertEquals(ExceptionDefinition.INVALID_ACCESS_CREDENTIAL.message, exception.message)
    }

}
