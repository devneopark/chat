package com.devneopark.chat.restapi.context.user.application.service

import com.devneopark.chat.lib.domain.user.model.Credential
import com.devneopark.chat.lib.domain.user.model.Profile
import com.devneopark.chat.lib.domain.user.model.User
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.user.application.exception.DuplicatedPrincipalException
import com.devneopark.chat.restapi.context.user.application.port.inbound.RegisterUserUseCase
import com.devneopark.chat.restapi.context.user.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.user.application.port.outbound.UserRepositoryPort
import com.devneopark.chat.restapi.context.user.application.policy.UserCredentialPolicy
import com.devneopark.chat.restapi.context.user.application.policy.UserProfilePolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserService(

    private val userRepositoryPort: UserRepositoryPort,

    private val idGenerator: IdGenerator,

    private val userCredentialPolicy: UserCredentialPolicy,

    private val userProfilePolicy: UserProfilePolicy,

    private val passwordHasher: PasswordHasher

) : RegisterUserUseCase {

    @Transactional
    override suspend fun register(command: RegisterUserUseCase.Command): RegisterUserUseCase.Result {
        userCredentialPolicy.validatePrincipal(command.principal)
        userCredentialPolicy.validatePassword(command.rawPassword)
        userProfilePolicy.validateDisplayName(command.displayName)

        val isPrincipalDuplicated = userRepositoryPort.existsByPrincipal(command.principal)
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
        userRepositoryPort.insert(user)

        return RegisterUserUseCase.Result(idValue)
    }

}
