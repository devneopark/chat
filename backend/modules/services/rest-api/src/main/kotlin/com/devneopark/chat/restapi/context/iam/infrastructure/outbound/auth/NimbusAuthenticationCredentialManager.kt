package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

import com.devneopark.chat.lib.domain.authentication_grant.model.AccessCredential
import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.authentication_grant.model.RenewalCredential
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Component
class NimbusAuthenticationCredentialManager(

    private val jwtProperties: JwtProperties,

    private val jwtEncoder: JwtEncoder,

    private val renewalCredentialProperties: RenewalCredentialProperties

) : AuthenticationCredentialManager {

    override suspend fun issue(
        grantId: AuthenticationGrant.Id,
        userId: UserId,
        now: Instant
    ): AuthenticationCredentialManager.CredentialSet {
        val header = JwsHeader.with(MacAlgorithm.HS512)
            .type(jwtProperties.type)
            .build()
        val jti = UUID.randomUUID().toString()
        val accessCredentialExpiresAt = now + jwtProperties.ttlMillis.milliseconds
        val claims = JwtClaimsSet.builder()
            .id(jti)
            .subject(userId.value)
            .issuedAt(now.toJavaInstant())
            .expiresAt(accessCredentialExpiresAt.toJavaInstant())
            .build()
        val parameters = JwtEncoderParameters.from(header, claims)
        val jwt = jwtEncoder.encode(parameters)

        val accessCredentialId = AccessCredential.Id(jti)
        val renewedId = UUID.randomUUID().toString()
        val renewalCredentialId = RenewalCredential.Id(renewedId)
        val renewalCredentialExpiresAt = now + renewalCredentialProperties.ttlMillis.milliseconds
        val authenticationGrant = AuthenticationGrant(
            grantId,
            userId,
            now,
            AccessCredential(
                accessCredentialId,
                now,
                accessCredentialExpiresAt
            ),
            RenewalCredential(
                renewalCredentialId,
                now,
                renewalCredentialExpiresAt
            )
        )
        return AuthenticationCredentialManager.CredentialSet(
            authenticationGrant,
            jwt.tokenValue,
            renewedId,
            accessCredentialExpiresAt,
            renewalCredentialExpiresAt
        )
    }

    @Component
    data class JwtProperties (

        @Value($$"${chat.infrastructure.auth.jwt.type}")
        val type: String,

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
