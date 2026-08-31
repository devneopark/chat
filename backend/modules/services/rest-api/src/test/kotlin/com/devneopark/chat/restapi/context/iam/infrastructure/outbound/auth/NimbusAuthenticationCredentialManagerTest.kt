package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.user.model.User
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class NimbusAuthenticationCredentialManagerTest {

    @Test
    fun `인증 정보를 발급하면 사용자 식별자를 담은 서명된 access JWT와 renewal credential을 반환한다`() = runTest {
        // given
        val encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk="
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            encodedSecretKey = encodedSecretKey,
            ttlMillis = 30.seconds.inWholeMilliseconds
        )
        val renewalCredentialProperties = NimbusAuthenticationCredentialManager.RenewalCredentialProperties(
            ttlMillis = 7.minutes.inWholeMilliseconds
        )
        val secretKey = SecretKeySpec(
            Base64.getDecoder().decode(jwtProperties.encodedSecretKey),
            "HmacSHA512"
        )
        val manager = NimbusAuthenticationCredentialManager(
            jwtProperties,
            NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS512)
                .build(),
            renewalCredentialProperties
        )
        val grantId = AuthenticationGrant.Id("grant-001")
        val userId = User.Id("user-001")
        val issuedAt = Instant.fromEpochSeconds(System.currentTimeMillis() / 1_000)

        // when
        val result = manager.issue(grantId, userId, issuedAt)

        // then
        val authenticationGrant = result.authenticationGrant
        val accessCredential = authenticationGrant.accessCredential
        val renewalCredential = authenticationGrant.renewalCredential
        assertTrue(result.serializedCredentialValue.isNotBlank())
        assertTrue(result.serializedRenewalCredentialValue.isNotBlank())
        assertNotEquals(accessCredential.id.value, renewalCredential.id.value)
        assertEquals(result.serializedRenewalCredentialValue, renewalCredential.id.value)
        assertEquals(issuedAt + 30.seconds, result.accessCredentialExpiresAt)
        assertEquals(issuedAt + 7.minutes, result.renewalCredentialExpiresAt)
        assertEquals(grantId, authenticationGrant.id)
        assertEquals(userId, authenticationGrant.userId)
        assertEquals(issuedAt, authenticationGrant.issuedAt)
        assertEquals(issuedAt, accessCredential.issuedAt)
        assertEquals(result.accessCredentialExpiresAt, accessCredential.willExpiresAt)
        assertEquals(issuedAt, renewalCredential.issuedAt)
        assertEquals(result.renewalCredentialExpiresAt, renewalCredential.willExpiresAt)

        val decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS512)
            .build()
        val jwt = decoder.decode(result.serializedCredentialValue)
        assertEquals(userId.value, jwt.subject)
        assertEquals(accessCredential.id.value, jwt.id)
        assertEquals(issuedAt.toJavaInstant(), jwt.issuedAt)
        assertEquals(result.accessCredentialExpiresAt.toJavaInstant(), jwt.expiresAt)
        assertEquals("JWT", jwt.headers["typ"])
        assertEquals(MacAlgorithm.HS512.name, jwt.headers["alg"])
    }

}
