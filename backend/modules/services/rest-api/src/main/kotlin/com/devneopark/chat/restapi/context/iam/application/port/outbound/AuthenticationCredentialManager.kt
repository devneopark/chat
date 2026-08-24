package com.devneopark.chat.restapi.context.iam.application.port.outbound

import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.time.Instant

interface AuthenticationCredentialManager {

    suspend fun issue(userId: UserId, now: Instant): CredentialSet

    data class CredentialSet(

        val accessCredentialInfo: AccessCredentialInfo,

        val renewalCredentialInfo: RenewalCredentialInfo

    )

    data class AccessCredentialInfo(

        val serializedValue: String,

        val id: String,

        val expiresAt: Instant

    )

    data class RenewalCredentialInfo(

        val id: String,

        val expiresAt: Instant

    )

}