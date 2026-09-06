package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

import com.devneopark.chat.restapi.framework.config.NimbusConfig
import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Instant
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NimbusAccessCredentialVerifierTest {

    @Test
    fun `유효한 access JWT를 검증하고 userId와 jti를 반환한다`() = runTest {
        // given
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            type = "JWT",
            encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk=",
            ttlMillis = 30_000
        )
        val secretKey = SecretKeySpec(
            Base64.getDecoder().decode(jwtProperties.encodedSecretKey),
            "HmacSHA512"
        )
        val encoder = NimbusJwtEncoder.withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS512)
            .build()
        val serializedCredential = encoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS512)
                    .type(jwtProperties.type)
                    .build(),
                JwtClaimsSet.builder()
                    .subject("user-001")
                    .id("access-jti-001")
                    .issuedAt(Instant.parse("2099-08-11T00:00:00Z"))
                    .expiresAt(Instant.parse("2099-08-11T00:30:00Z"))
                    .build()
            )
        ).tokenValue
        val verifier = NimbusAccessCredentialVerifier(
            NimbusConfig().jwtDecoder(jwtProperties)
        )

        // when
        val result = verifier.verify(serializedCredential)

        // then
        assertEquals("user-001", result.userId)
        assertEquals("access-jti-001", result.jti)
    }

    @Test
    fun `다른 secret으로 서명된 access JWT는 검증하지 않는다`() = runTest {
        // given
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            type = "JWT",
            encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk=",
            ttlMillis = 30_000
        )
        val anotherSecretKey = SecretKeySpec(
            ByteArray(64) { 1 },
            "HmacSHA512"
        )
        val serializedCredential = NimbusJwtEncoder.withSecretKey(anotherSecretKey)
            .algorithm(MacAlgorithm.HS512)
            .build()
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS512)
                        .type(jwtProperties.type)
                        .build(),
                    JwtClaimsSet.builder()
                        .subject("user-001")
                        .id("access-jti-001")
                        .expiresAt(Instant.parse("2099-08-11T00:30:00Z"))
                        .build()
                )
            ).tokenValue
        val verifier = NimbusAccessCredentialVerifier(
            NimbusConfig().jwtDecoder(jwtProperties)
        )

        // when
        val exception = assertFailsWith<InvalidAccessCredentialException> {
            verifier.verify(serializedCredential)
        }

        // then
        assertEquals("2-001-005", exception.code)
        assertEquals("Invalid access credential.", exception.message)
    }

    @Test
    fun `만료된 access JWT는 검증하지 않는다`() = runTest {
        // given
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            type = "JWT",
            encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk=",
            ttlMillis = 30_000
        )
        val secretKey = SecretKeySpec(
            Base64.getDecoder().decode(jwtProperties.encodedSecretKey),
            "HmacSHA512"
        )
        val serializedCredential = NimbusJwtEncoder.withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS512)
            .build()
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS512)
                        .type(jwtProperties.type)
                        .build(),
                    JwtClaimsSet.builder()
                        .subject("user-001")
                        .id("access-jti-001")
                        .issuedAt(Instant.parse("2000-08-10T00:00:00Z"))
                        .expiresAt(Instant.parse("2000-08-11T00:00:00Z"))
                        .build()
                )
            ).tokenValue
        val verifier = NimbusAccessCredentialVerifier(
            NimbusConfig().jwtDecoder(jwtProperties)
        )

        // when
        val exception = assertFailsWith<InvalidAccessCredentialException> {
            verifier.verify(serializedCredential)
        }

        // then
        assertEquals("2-001-005", exception.code)
        assertEquals("Invalid access credential.", exception.message)
    }

    @Test
    fun `subject가 없는 access JWT는 검증하지 않는다`() = runTest {
        // given
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            type = "JWT",
            encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk=",
            ttlMillis = 30_000
        )
        val secretKey = SecretKeySpec(
            Base64.getDecoder().decode(jwtProperties.encodedSecretKey),
            "HmacSHA512"
        )
        val serializedCredential = NimbusJwtEncoder.withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS512)
            .build()
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS512)
                        .type(jwtProperties.type)
                        .build(),
                    JwtClaimsSet.builder()
                        .id("access-jti-001")
                        .expiresAt(Instant.parse("2099-08-11T00:30:00Z"))
                        .build()
                )
            ).tokenValue
        val verifier = NimbusAccessCredentialVerifier(
            NimbusConfig().jwtDecoder(jwtProperties)
        )

        // when
        val exception = assertFailsWith<InvalidAccessCredentialException> {
            verifier.verify(serializedCredential)
        }

        // then
        assertEquals("2-001-005", exception.code)
        assertEquals("Invalid access credential.", exception.message)
    }

    @Test
    fun `jti가 없는 access JWT는 검증하지 않는다`() = runTest {
        // given
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            type = "JWT",
            encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk=",
            ttlMillis = 30_000
        )
        val secretKey = SecretKeySpec(
            Base64.getDecoder().decode(jwtProperties.encodedSecretKey),
            "HmacSHA512"
        )
        val serializedCredential = NimbusJwtEncoder.withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS512)
            .build()
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS512)
                        .type(jwtProperties.type)
                        .build(),
                    JwtClaimsSet.builder()
                        .subject("user-001")
                        .expiresAt(Instant.parse("2099-08-11T00:30:00Z"))
                        .build()
                )
            ).tokenValue
        val verifier = NimbusAccessCredentialVerifier(
            NimbusConfig().jwtDecoder(jwtProperties)
        )

        // when
        val exception = assertFailsWith<InvalidAccessCredentialException> {
            verifier.verify(serializedCredential)
        }

        // then
        assertEquals("2-001-005", exception.code)
        assertEquals("Invalid access credential.", exception.message)
    }

}
