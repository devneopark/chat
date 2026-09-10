package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.lib.domain.user.service.UserCredentialValidator
import com.devneopark.chat.lib.domain.user.service.UserProfileValidator
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.DuplicatedPrincipalException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RegisterUserUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.iam.application.port.outbound.IamUserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserService(

    private val iamUserRepositoryPort: IamUserRepositoryPort,

    private val idGenerator: IdGenerator,

    private val userCredentialValidator: UserCredentialValidator,

    private val userProfileValidator: UserProfileValidator,

    private val passwordHasher: PasswordHasher

) : RegisterUserUseCase {

    @Transactional
    override suspend fun register(command: RegisterUserUseCase.Command): RegisterUserUseCase.Result {
        userCredentialValidator.validatePrincipal(command.principal)
        userCredentialValidator.validatePassword(command.rawPassword)
        userProfileValidator.validateDisplayName(command.displayName)

        val isPrincipalDuplicated = iamUserRepositoryPort.existsByPrincipal(command.principal)
        if (isPrincipalDuplicated) {
            throw DuplicatedPrincipalException()
        }

        val idValue = idGenerator.generate()
        val passwordHash = passwordHasher.hash(command.rawPassword)
        val user = User(
            User.Id(idValue),
            Credential(command.principal, passwordHash),
            Profile(command.displayName)
        )
        iamUserRepositoryPort.insert(user)

        return RegisterUserUseCase.Result(idValue)
    }

}
