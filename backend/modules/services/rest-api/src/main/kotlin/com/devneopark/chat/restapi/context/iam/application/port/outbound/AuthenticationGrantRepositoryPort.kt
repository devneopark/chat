package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant

interface AuthenticationGrantRepositoryPort {

    // Create
    suspend fun insert(authenticationGrant: AuthenticationGrant): AuthenticationGrant

    // Read
    suspend fun findByJti(jti: String): AuthenticationGrant?

    suspend fun findByRenewalCredentialId(renewalCredentialId: String): AuthenticationGrant?

    // Update

    // Delete
    suspend fun deleteByJti(jti: String)

    suspend fun deleteByRenewalCredentialId(renewalCredentialId: String)

}