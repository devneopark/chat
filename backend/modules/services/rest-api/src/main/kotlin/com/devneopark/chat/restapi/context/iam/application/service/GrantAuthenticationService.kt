package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.user.service.UserCredentialValidator
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.iam.application.exception.WrongPasswordException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.GrantAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationCredentialManager
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.iam.application.port.outbound.IamUserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

@Service
class GrantAuthenticationService(

    private val userCredentialValidator: UserCredentialValidator,

    private val iamUserRepositoryPort: IamUserRepositoryPort,

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

        val user = iamUserRepositoryPort.findByPrincipal(command.principal)
            ?: run {
                throw UserNotFoundException()
            }

        val passwordHash = user.credential.passwordHash
        val isPasswordMatches = passwordHasher.matches(command.rawPassword, passwordHash)
        if (!isPasswordMatches) {
            throw WrongPasswordException()
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
