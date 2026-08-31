package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.time.Instant

interface AuthenticationCredentialManager {

    suspend fun issue(grantId: AuthenticationGrant.Id, userId: UserId, now: Instant): CredentialSet

    data class CredentialSet(

        val authenticationGrant: AuthenticationGrant,

        val serializedCredentialValue: String,

        val serializedRenewalCredentialValue: String,

        val accessCredentialExpiresAt: Instant,

        val renewalCredentialExpiresAt: Instant

    )

}