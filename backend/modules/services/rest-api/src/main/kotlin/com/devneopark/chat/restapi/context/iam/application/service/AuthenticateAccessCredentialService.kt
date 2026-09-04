package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.restapi.context.iam.application.exception.ExceptionDefinition
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AccessCredentialVerifier
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

@Service
class AuthenticateAccessCredentialService(

    private val accessCredentialVerifier: AccessCredentialVerifier,

    private val authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort,

    private val clock: Clock

) : AuthenticateAccessCredentialUseCase {

    @Transactional
    override suspend fun authenticate(command: AuthenticateAccessCredentialUseCase.Command): AuthenticateAccessCredentialUseCase.Result {
        val verifiedCredential = accessCredentialVerifier.verify(command.serializedCredential)
        val authenticationGrant = authenticationGrantRepositoryPort.findByJti(verifiedCredential.jti)
            ?: run {
                val exceptionDefinition = ExceptionDefinition.INVALID_ACCESS_CREDENTIAL
                throw IamContextException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }

        if (authenticationGrant.userId.value != verifiedCredential.userId) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ACCESS_CREDENTIAL
            throw IamContextException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }

        val now = clock.instant().toKotlinInstant()
        if (!authenticationGrant.isAccessCredentialUsable(now)) {
            val exceptionDefinition = ExceptionDefinition.INVALID_ACCESS_CREDENTIAL
            throw IamContextException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }

        return AuthenticateAccessCredentialUseCase.Result(
            authenticationGrant.userId.value,
            verifiedCredential.jti
        )
    }

}
