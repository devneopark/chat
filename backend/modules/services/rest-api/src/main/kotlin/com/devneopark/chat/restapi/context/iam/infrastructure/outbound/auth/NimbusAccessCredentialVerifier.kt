package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth

import com.devneopark.chat.restapi.context.iam.application.exception.ExceptionDefinition
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
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
            val exceptionDefinition = ExceptionDefinition.INVALID_ACCESS_CREDENTIAL
            throw IamContextException(
                exceptionDefinition.code,
                exceptionDefinition.message,
                exception
            )
        }

        val userId = jwt.subject
        val jti = jwt.id
        if (userId == null || jti == null) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ACCESS_CREDENTIAL
            throw IamContextException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }

        return AccessCredentialVerifier.VerifiedCredential(userId, jti)
    }

}
