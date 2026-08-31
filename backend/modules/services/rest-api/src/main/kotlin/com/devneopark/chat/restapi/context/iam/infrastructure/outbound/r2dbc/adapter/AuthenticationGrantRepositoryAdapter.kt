package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.adapter

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model.AuthenticationGrantEntity
import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.repository.AuthenticationGrantEntityRepository
import org.springframework.stereotype.Repository

@Repository
class AuthenticationGrantRepositoryAdapter(

    private val authenticationGrantEntityRepository: AuthenticationGrantEntityRepository

) : AuthenticationGrantRepositoryPort {

    override suspend fun insert(authenticationGrant: AuthenticationGrant): AuthenticationGrant {
        val entity = AuthenticationGrantEntity.from(authenticationGrant)
        authenticationGrantEntityRepository.insert(entity)
        return authenticationGrant
    }

    override suspend fun findByJti(jti: String): AuthenticationGrant? {
        return authenticationGrantEntityRepository.findByAcJti(jti)
            ?.toDomain()
    }

    override suspend fun findByRenewalCredentialId(renewalCredentialId: String): AuthenticationGrant? {
        return authenticationGrantEntityRepository.findByRcId(renewalCredentialId)
            ?.toDomain()
    }

    override suspend fun deleteByJti(jti: String) {
        authenticationGrantEntityRepository.deleteByAcJti(jti)
    }

    override suspend fun deleteByRenewalCredentialId(renewalCredentialId: String) {
        authenticationGrantEntityRepository.deleteByRcId(renewalCredentialId)
    }

}