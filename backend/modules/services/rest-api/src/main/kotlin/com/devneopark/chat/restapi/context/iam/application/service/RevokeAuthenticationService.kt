package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.restapi.context.iam.application.port.inbound.RevokeAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RevokeAuthenticationService(

    private val authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort

) : RevokeAuthenticationUseCase {

    @Transactional
    override suspend fun revoke(command: RevokeAuthenticationUseCase.Command) {
        authenticationGrantRepositoryPort.deleteByJti(command.jti)
    }

}
