package com.devneopark.chat.restapi.bootstrap.config

import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth.NimbusAuthenticationCredentialManager
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals

class NimbusConfigTest {

    @Test
    fun `base64 secret으로 HS512 JWT encoder를 생성한다`() {
        // given
        val jwtProperties = NimbusAuthenticationCredentialManager.JwtProperties(
            encodedSecretKey = "VjFSS2IyRkhVa1JOV0U1cFRXczFiMWxyVFhoalYxRjZWVmhTYWsxdGVIVlpiVEZ6WkZad05VMVljR0ZXTURVMVYyeG9VbVJIUlhsV2FsVTk=",
            ttlMillis = 30_000
        )
        val secretKey = SecretKeySpec(
            Base64.getDecoder().decode(jwtProperties.encodedSecretKey),
            "HmacSHA512"
        )

        // when
        val encodedJwt = NimbusConfig()
            .jwtEncoder(jwtProperties)
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS512)
                        .type("JWT")
                        .build(),
                    JwtClaimsSet.builder()
                        .subject("user-001")
                        .build()
                )
            )

        // then
        val jwt = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS512)
            .build()
            .decode(encodedJwt.tokenValue)
        assertEquals("user-001", jwt.subject)
        assertEquals(MacAlgorithm.HS512.name, jwt.headers["alg"])
    }

}
