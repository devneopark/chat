package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.user.service.UserCredentialValidator
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.ExceptionDefinition
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.GrantAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationCredentialManager
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.iam.application.port.outbound.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

@Service
class GrantAuthenticationService(

    private val userCredentialValidator: UserCredentialValidator,

    private val userRepositoryPort: UserRepositoryPort,

    private val passwordHasher: PasswordHasher,

    private val clock: Clock,

    private val authenticationCredentialManager: AuthenticationCredentialManager,

    private val idGenerator: IdGenerator,

    private val authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort

) : GrantAuthenticationUseCase {

    @Transactional
    override suspend fun grant(command: GrantAuthenticationUseCase.Command): GrantAuthenticationUseCase.Result {
        userCredentialValidator.validatePrincipal(command.principal)
        userCredentialValidator.validatePassword(command.rawPassword)

        val user = userRepositoryPort.findByPrincipal(command.principal)
            ?: run {
                val exceptionDefinition = ExceptionDefinition.USER_NOT_FOUND
                throw IamContextException(
                    exceptionDefinition.code,
                    exceptionDefinition.message
                )
            }

        val passwordHash = user.credential.passwordHash
        val isPasswordMatches = passwordHasher.matches(command.rawPassword, passwordHash)
        if (!isPasswordMatches) {
            val exceptionDefinition = ExceptionDefinition.WRONG_PASSWORD
            throw IamContextException(
                exceptionDefinition.code,
                exceptionDefinition.message
            )
        }

        val now = clock.instant().toKotlinInstant()
        val grantId = AuthenticationGrant.Id(idGenerator.generate())
        val credentialSet = authenticationCredentialManager.issue(grantId, user.id, now)
        authenticationGrantRepositoryPort.insert(credentialSet.authenticationGrant)

        return GrantAuthenticationUseCase.Result(
            user.id.value,
            GrantAuthenticationUseCase.Credential(
                credentialSet.serializedCredentialValue,
                credentialSet.accessCredentialExpiresAt
            ),
            GrantAuthenticationUseCase.Credential(
                credentialSet.authenticationGrant.renewalCredential.id.value,
                credentialSet.renewalCredentialExpiresAt
            )
        )
    }

}