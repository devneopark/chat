package com.devneopark.chat.lib.domain.authentication_grant.model

import com.devneopark.chat.lib.domain.user.reference.UserId
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthenticationGrantTest {

    @Test
    fun `given user and credentials when grant is created then current credentials are preserved`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val accessCredential = AccessCredential(
            id = AccessCredential.Id("access-1"),
            issuedAt = issuedAt,
            willExpiresAt = issuedAt + 15.minutes
        )
        val renewalCredential = RenewalCredential(
            id = RenewalCredential.Id("renewal-1"),
            issuedAt = issuedAt,
            willExpiresAt = issuedAt + 7.days
        )

        // when
        val grant = AuthenticationGrant(
            id = AuthenticationGrant.Id("grant-1"),
            userId = TestUserId("user-1"),
            issuedAt = issuedAt,
            accessCredential = accessCredential,
            renewalCredential = renewalCredential
        )

        // then
        assertEquals("user-1", grant.userId.value)
        assertEquals(accessCredential, grant.accessCredential)
        assertEquals(renewalCredential, grant.renewalCredential)
    }

    @Test
    fun `given blank referenced user id when grant is created then grant accepts it`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")

        // when
        val grant = AuthenticationGrant(
            id = AuthenticationGrant.Id("grant-1"),
            userId = TestUserId(""),
            issuedAt = issuedAt,
            accessCredential = AccessCredential(
                id = AccessCredential.Id("access-1"),
                issuedAt = issuedAt,
                willExpiresAt = issuedAt + 15.minutes
            ),
            renewalCredential = RenewalCredential(
                id = RenewalCredential.Id("renewal-1"),
                issuedAt = issuedAt,
                willExpiresAt = issuedAt + 7.days
            )
        )

        // then
        assertEquals("", grant.userId.value)
    }

    @Test
    fun `given active access credential when credential is validated then validation succeeds`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val accessCredential = AccessCredential(
            id = AccessCredential.Id("access-1"),
            issuedAt = issuedAt,
            willExpiresAt = issuedAt + 15.minutes
        )
        val renewalCredential = RenewalCredential(
            id = RenewalCredential.Id("renewal-1"),
            issuedAt = issuedAt,
            willExpiresAt = issuedAt + 7.days
        )
        val grant = AuthenticationGrant(
            id = AuthenticationGrant.Id("grant-1"),
            userId = TestUserId("user-1"),
            issuedAt = issuedAt,
            accessCredential = accessCredential,
            renewalCredential = renewalCredential
        )

        // when
        val result = grant.isAccessCredentialUsable(issuedAt + 1.minutes)

        // then
        assertTrue(result)
        assertEquals(accessCredential, grant.accessCredential)
        assertEquals(renewalCredential, grant.renewalCredential)
    }

    @Test
    fun `given expired access credential when credential is validated then validation fails`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val grant = AuthenticationGrant(
            id = AuthenticationGrant.Id("grant-1"),
            userId = TestUserId("user-1"),
            issuedAt = issuedAt,
            accessCredential = AccessCredential(
                id = AccessCredential.Id("access-1"),
                issuedAt = issuedAt,
                willExpiresAt = issuedAt + 15.minutes
            ),
            renewalCredential = RenewalCredential(
                id = RenewalCredential.Id("renewal-1"),
                issuedAt = issuedAt,
                willExpiresAt = issuedAt + 7.days
            )
        )

        // when
        val result = grant.isAccessCredentialUsable(issuedAt + 15.minutes)

        // then
        assertFalse(result)
    }

    @Test
    fun `given active renewal credential when credential is validated then validation succeeds`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val renewalCredential = RenewalCredential(
            id = RenewalCredential.Id("renewal-1"),
            issuedAt = issuedAt,
            willExpiresAt = issuedAt + 7.days
        )
        val grant = AuthenticationGrant(
            id = AuthenticationGrant.Id("grant-1"),
            userId = TestUserId("user-1"),
            issuedAt = issuedAt,
            accessCredential = AccessCredential(
                id = AccessCredential.Id("access-1"),
                issuedAt = issuedAt,
                willExpiresAt = issuedAt + 15.minutes
            ),
            renewalCredential = renewalCredential
        )

        // when
        val result = grant.isRenewalCredentialUsable(issuedAt + 1.minutes)

        // then
        assertTrue(result)
        assertEquals(renewalCredential, grant.renewalCredential)
    }

    @Test
    fun `given expired renewal credential when credential is validated then validation fails`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val renewalWillExpireAt = issuedAt + 7.days
        val grant = AuthenticationGrant(
            id = AuthenticationGrant.Id("grant-1"),
            userId = TestUserId("user-1"),
            issuedAt = issuedAt,
            accessCredential = AccessCredential(
                id = AccessCredential.Id("access-1"),
                issuedAt = issuedAt,
                willExpiresAt = issuedAt + 15.minutes
            ),
            renewalCredential = RenewalCredential(
                id = RenewalCredential.Id("renewal-1"),
                issuedAt = issuedAt,
                willExpiresAt = renewalWillExpireAt
            )
        )

        // when
        val result = grant.isRenewalCredentialUsable(renewalWillExpireAt)

        // then
        assertFalse(result)
    }

    private data class TestUserId(
        override val value: String
    ) : UserId

}
