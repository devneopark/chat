package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.r2dbc.model

import com.devneopark.chat.lib.domain.authentication_grant.model.AccessCredential
import com.devneopark.chat.lib.domain.authentication_grant.model.AuthenticationGrant
import com.devneopark.chat.lib.domain.authentication_grant.model.RenewalCredential
import com.devneopark.chat.lib.domain.user.model.User
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.test.assertEquals

class AuthenticationGrantEntityTest {

    @Test
    fun `AuthenticationGrant를 AuthenticationGrantEntity로 변환한다`() {
        // given
        val issuedAt = kotlin.time.Instant.parse("2026-08-11T00:00:00Z")
        val accessIssuedAt = kotlin.time.Instant.parse("2026-08-11T00:00:01Z")
        val renewalIssuedAt = kotlin.time.Instant.parse("2026-08-11T00:00:02Z")
        val accessCredential = AccessCredential(
            AccessCredential.Id("access-jti-001"),
            accessIssuedAt,
            accessIssuedAt + 15.minutes
        )
        val renewalCredential = RenewalCredential(
            RenewalCredential.Id("renewal-id-001"),
            renewalIssuedAt,
            renewalIssuedAt + 7.days
        )
        val authenticationGrant = AuthenticationGrant(
            AuthenticationGrant.Id("grant-001"),
            User.Id("user-001"),
            issuedAt,
            accessCredential,
            renewalCredential
        )

        // when
        val entity = AuthenticationGrantEntity.from(authenticationGrant)

        // then
        assertEquals("grant-001", entity.id)
        assertEquals("user-001", entity.userId)
        assertEquals(issuedAt.toJavaInstant(), entity.issuedAt)
        assertEquals("access-jti-001", entity.acJti)
        assertEquals(accessIssuedAt.toJavaInstant(), entity.acIssuedAt)
        assertEquals(accessCredential.willExpiresAt.toJavaInstant(), entity.acWillExpiresAt)
        assertEquals("renewal-id-001", entity.rcId)
        assertEquals(renewalIssuedAt.toJavaInstant(), entity.rcIssuedAt)
        assertEquals(renewalCredential.willExpiresAt.toJavaInstant(), entity.rcWillExpiresAt)
    }

    @Test
    fun `AuthenticationGrantEntity를 AuthenticationGrant로 변환한다`() {
        // given
        val entity = AuthenticationGrantEntity().apply {
            id = "grant-001"
            userId = "user-001"
            issuedAt = Instant.parse("2026-08-11T00:00:00Z")
            acJti = "access-jti-001"
            acIssuedAt = Instant.parse("2026-08-11T00:00:01Z")
            acWillExpiresAt = Instant.parse("2026-08-11T00:15:01Z")
            rcId = "renewal-id-001"
            rcIssuedAt = Instant.parse("2026-08-11T00:00:02Z")
            rcWillExpiresAt = Instant.parse("2026-08-18T00:00:02Z")
        }

        // when
        val authenticationGrant = entity.toDomain()

        // then
        assertEquals("grant-001", authenticationGrant.id.value)
        assertEquals("user-001", authenticationGrant.userId.value)
        assertEquals(entity.issuedAt.toKotlinInstant(), authenticationGrant.issuedAt)
        assertEquals("access-jti-001", authenticationGrant.accessCredential.id.value)
        assertEquals(entity.acIssuedAt.toKotlinInstant(), authenticationGrant.accessCredential.issuedAt)
        assertEquals(entity.acWillExpiresAt.toKotlinInstant(), authenticationGrant.accessCredential.willExpiresAt)
        assertEquals("renewal-id-001", authenticationGrant.renewalCredential.id.value)
        assertEquals(entity.rcIssuedAt.toKotlinInstant(), authenticationGrant.renewalCredential.issuedAt)
        assertEquals(entity.rcWillExpiresAt.toKotlinInstant(), authenticationGrant.renewalCredential.willExpiresAt)
    }

}
