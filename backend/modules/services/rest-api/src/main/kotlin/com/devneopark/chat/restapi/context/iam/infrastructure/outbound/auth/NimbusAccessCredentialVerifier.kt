package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AccessCredentialVerifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class NimbusAccessCredentialVerifier(

    private val jwtDecoder: JwtDecoder

) : AccessCredentialVerifier {

    override suspend fun verify(serializedCredential: String): AccessCredentialVerifier.VerifiedCredential {
        val jwt = try {
            jwtDecoder.decode(serializedCredential)
        } catch (exception: JwtException) {
            throw InvalidAccessCredentialException(exception)
        }

        val userId = jwt.subject
        val jti = jwt.id
        if (userId == null || jti == null) {
            throw InvalidAccessCredentialException()
        }

        return AccessCredentialVerifier.VerifiedCredential(userId, jti)
    }

}
