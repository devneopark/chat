package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant

/** AuthenticationGrant의 현재 상태를 영속화하고 credential 식별자로 조회하는 계약이다. */
interface AuthenticationGrantRepositoryPort {

    // Create
    /** 새로운 인증정보를 저장한다. */
    suspend fun insert(authenticationGrant: AuthenticationGrant): AuthenticationGrant

    // Read
    /** access credential의 JTI로 인증정보를 조회한다. */
    suspend fun findByJti(jti: String): AuthenticationGrant?

    /**
     * renewal credential로 인증정보를 조회하면서 해당 행을 잠근다.
     *
     * 호출자는 기존 Grant 삭제와 새 Grant 저장을 같은 트랜잭션에서 수행해야 한다.
     */
    suspend fun findByRenewalCredentialIdForUpdate(renewalCredentialId: String): AuthenticationGrant?

    // Update

    // Delete
    /** access credential의 JTI에 해당하는 인증정보를 삭제한다. */
    suspend fun deleteByJti(jti: String)

    /** renewal credential에 해당하는 인증정보를 삭제한다. */
    suspend fun deleteByRenewalCredentialId(renewalCredentialId: String)

}
