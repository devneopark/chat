package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationCredentialManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class NimbusAuthenticationCredentialManager(

    private val jwtProperties: JwtProperties,

    private val jwtEncoder: JwtEncoder,

    private val renewalCredentialProperties: RenewalCredentialProperties

) : AuthenticationCredentialManager {

    override suspend fun issue(
        userId: UserId,
        now: Instant
    ): AuthenticationCredentialManager.CredentialSet {
        val header = JwsHeader.with(MacAlgorithm.HS512)
            .type("JWT")
            .build()
        val jti = UUID.randomUUID().toString()
        val now = now.toJavaInstant()
        val expiresAt = now.plusMillis(jwtProperties.ttlMillis)
        val claims = JwtClaimsSet.builder()
            .id(jti)
            .subject(userId.value)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .build()
        val parameters = JwtEncoderParameters.from(header, claims)
        val jwt = jwtEncoder.encode(parameters)
        val accessCredentialInfo = AuthenticationCredentialManager.AccessCredentialInfo(
            jwt.tokenValue,
            jti,
            expiresAt.toKotlinInstant()
        )

        val renewalCredentialId = UUID.randomUUID().toString()
        val renewalCredentialExpiresAt = now.plusMillis(renewalCredentialProperties.ttlMillis)
        val renewalCredentialInfo = AuthenticationCredentialManager.RenewalCredentialInfo(
            renewalCredentialId,
            renewalCredentialExpiresAt.toKotlinInstant()
        )

        return AuthenticationCredentialManager.CredentialSet(accessCredentialInfo, renewalCredentialInfo)
    }

    @Component
    data class JwtProperties (

        @Value($$"${chat.infrastructure.auth.jwt.secretKey}")
        val encodedSecretKey: String,

        @Value($$"${chat.infrastructure.auth.jwt.ttlMillis}")
        val ttlMillis: Long

    )

    @Component
    data class RenewalCredentialProperties (

        @Value($$"${chat.infrastructure.auth.refreshToken.ttlMillis}")
        val ttlMillis: Long

    )

}
