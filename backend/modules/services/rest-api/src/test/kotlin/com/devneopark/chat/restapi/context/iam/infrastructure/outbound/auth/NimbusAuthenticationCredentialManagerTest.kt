package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

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
        val userId = User.Id("user-001")
        val issuedAt = Instant.fromEpochSeconds(System.currentTimeMillis() / 1_000)

        // when
        val result = manager.issue(userId, issuedAt)

        // then
        val accessCredential = result.accessCredentialInfo
        val renewalCredential = result.renewalCredentialInfo
        assertTrue(accessCredential.serializedValue.isNotBlank())
        assertTrue(accessCredential.id.isNotBlank())
        assertNotEquals(accessCredential.id, renewalCredential.id)
        assertEquals(issuedAt + 30.seconds, accessCredential.expiresAt)
        assertEquals(issuedAt + 7.minutes, renewalCredential.expiresAt)

        val decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS512)
            .build()
        val jwt = decoder.decode(accessCredential.serializedValue)
        assertEquals(userId.value, jwt.subject)
        assertEquals(accessCredential.id, jwt.id)
        assertEquals(issuedAt.toJavaInstant(), jwt.issuedAt)
        assertEquals(accessCredential.expiresAt.toJavaInstant(), jwt.expiresAt)
        assertEquals("JWT", jwt.headers["typ"])
        assertEquals(MacAlgorithm.HS512.name, jwt.headers["alg"])
    }

}
