package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.InvalidRenewalCredentialException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RenewalAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationCredentialManager
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.time.toKotlinInstant

@Service
class RenewalAuthenticationService(

    private val authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort,

    private val clock: Clock,

    private val authenticationCredentialManager: AuthenticationCredentialManager,

    private val idGenerator: IdGenerator

) : RenewalAuthenticationUseCase {

    @Transactional
    override suspend fun renewal(command: RenewalAuthenticationUseCase.Command): RenewalAuthenticationUseCase.Result {
        val renewalCredentialId = command.renewalCredentialId
        val authenticationGrant = authenticationGrantRepositoryPort.findByRenewalCredentialId(renewalCredentialId)
            ?: run {
                throw InvalidRenewalCredentialException()
            }

        val now = clock.instant().toKotlinInstant()
        val isUsableCredential = authenticationGrant.isRenewalCredentialUsable(now)
        if (!isUsableCredential) {
            throw InvalidRenewalCredentialException()
        }

        val userId = authenticationGrant.userId
        val grantId = AuthenticationGrant.Id(idGenerator.generate())
        val credentialSet = authenticationCredentialManager.issue(grantId, userId, now)

        authenticationGrantRepositoryPort.insert(credentialSet.authenticationGrant)
        authenticationGrantRepositoryPort.deleteByRenewalCredentialId(renewalCredentialId)

        return RenewalAuthenticationUseCase.Result(
            userId.value,
            RenewalAuthenticationUseCase.Credential(
                credentialSet.serializedCredentialValue,
                credentialSet.accessCredentialExpiresAt
            ),
            RenewalAuthenticationUseCase.Credential(
                credentialSet.authenticationGrant.renewalCredential.id.value,
                credentialSet.renewalCredentialExpiresAt
            )
        )
    }

}
