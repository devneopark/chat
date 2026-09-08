package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.authentication_grant.model.AccessCredential
import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.authentication_grant.model.RenewalCredential
import com.devneopark.chat.lib.domain.user.model.User
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Table("authentication_grant")
class AuthenticationGrantEntity {

    lateinit var id: String

    lateinit var userId: String

    lateinit var issuedAt: Instant

    lateinit var acJti: String

    lateinit var acIssuedAt: Instant

    lateinit var acWillExpiresAt: Instant

    lateinit var rcId: String

    lateinit var rcIssuedAt: Instant

    lateinit var rcWillExpiresAt: Instant

    companion object {

        fun from(authenticationGrant: AuthenticationGrant): AuthenticationGrantEntity {
            return AuthenticationGrantEntity().apply {
                id = authenticationGrant.id.value
                userId = authenticationGrant.userId.value
                issuedAt = authenticationGrant.issuedAt.toJavaInstant()
                acJti = authenticationGrant.accessCredential.id.value
                acIssuedAt = authenticationGrant.accessCredential.issuedAt.toJavaInstant()
                acWillExpiresAt = authenticationGrant.accessCredential.willExpiresAt.toJavaInstant()
                rcId = authenticationGrant.renewalCredential.id.value
                rcIssuedAt = authenticationGrant.renewalCredential.issuedAt.toJavaInstant()
                rcWillExpiresAt = authenticationGrant.renewalCredential.willExpiresAt.toJavaInstant()
            }
        }

    }

    fun toDomain(): AuthenticationGrant {
        val authenticationGrantId = AuthenticationGrant.Id.from(id)
        val userId = User.Id.from(userId)
        val accessCredentialId = AccessCredential.Id.from(acJti)
        val accessCredential = AccessCredential(
            accessCredentialId,
            acIssuedAt.toKotlinInstant(),
            acWillExpiresAt.toKotlinInstant(),
        )
        val renewalCredentialId = RenewalCredential.Id.from(rcId)
        val renewalCredential = RenewalCredential(
            renewalCredentialId,
            rcIssuedAt.toKotlinInstant(),
            rcWillExpiresAt.toKotlinInstant(),
        )
        return AuthenticationGrant(
            authenticationGrantId,
            userId,
            issuedAt.toKotlinInstant(),
            accessCredential,
            renewalCredential
        )
    }

}