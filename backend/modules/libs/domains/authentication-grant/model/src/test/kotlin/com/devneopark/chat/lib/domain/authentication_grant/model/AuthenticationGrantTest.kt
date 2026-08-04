package com.devneopark.chat.lib.domain.authentication_grant.model

import com.devneopark.chat.lib.domain.user.reference.UserId
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `given active renewal credential when rotating then current credentials are replaced`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val rotationTime = issuedAt + 1.seconds
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
        val nextAccessCredential = AccessCredential(
            id = AccessCredential.Id("access-2"),
            issuedAt = rotationTime,
            willExpiresAt = rotationTime + 15.minutes
        )
        val nextRenewalCredential = RenewalCredential(
            id = RenewalCredential.Id("renewal-2"),
            issuedAt = rotationTime,
            willExpiresAt = rotationTime + 7.days
        )

        // when
        grant.rotateCredentials(
            presentedRenewalCredentialId = RenewalCredential.Id("renewal-1"),
            nextAccessCredential = nextAccessCredential,
            nextRenewalCredential = nextRenewalCredential,
            now = rotationTime
        )

        // then
        assertEquals(nextAccessCredential, grant.accessCredential)
        assertEquals(nextRenewalCredential, grant.renewalCredential)
    }

    @Test
    fun `given expired renewal credential when rotating then request is rejected`() {
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
        val exception = assertFailsWith<DomainRuleViolationException> {
            grant.rotateCredentials(
                presentedRenewalCredentialId = RenewalCredential.Id("renewal-1"),
                nextAccessCredential = AccessCredential(
                    id = AccessCredential.Id("access-2"),
                    issuedAt = renewalWillExpireAt,
                    willExpiresAt = renewalWillExpireAt + 15.minutes
                ),
                nextRenewalCredential = RenewalCredential(
                    id = RenewalCredential.Id("renewal-2"),
                    issuedAt = renewalWillExpireAt,
                    willExpiresAt = renewalWillExpireAt + 7.days
                ),
                now = renewalWillExpireAt
            )
        }

        // then
        assertEquals("1-006-010", exception.code)
        assertEquals("access-1", grant.accessCredential.id.value)
        assertEquals("renewal-1", grant.renewalCredential.id.value)
    }

    @Test
    fun `given next credential issued before rotation when rotating then request is rejected`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val rotationTime = issuedAt + 1.seconds
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
        val exception = assertFailsWith<DomainRuleViolationException> {
            grant.rotateCredentials(
                presentedRenewalCredentialId = RenewalCredential.Id("renewal-1"),
                nextAccessCredential = AccessCredential(
                    id = AccessCredential.Id("access-2"),
                    issuedAt = issuedAt,
                    willExpiresAt = issuedAt + 15.minutes
                ),
                nextRenewalCredential = RenewalCredential(
                    id = RenewalCredential.Id("renewal-2"),
                    issuedAt = rotationTime,
                    willExpiresAt = rotationTime + 7.days
                ),
                now = rotationTime
            )
        }

        // then
        assertEquals("1-006-017", exception.code)
        assertEquals("access-1", grant.accessCredential.id.value)
        assertEquals("renewal-1", grant.renewalCredential.id.value)
    }

    @Test
    fun `given renewal credential that does not match current credential then request is rejected`() {
        // given
        val issuedAt = Instant.parse("2026-08-04T00:00:00Z")
        val rotationTime = issuedAt + 1.seconds
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
        val rotatedAccessCredential = AccessCredential(
            id = AccessCredential.Id("access-2"),
            issuedAt = rotationTime,
            willExpiresAt = rotationTime + 15.minutes
        )
        val rotatedRenewalCredential = RenewalCredential(
            id = RenewalCredential.Id("renewal-2"),
            issuedAt = rotationTime,
            willExpiresAt = rotationTime + 7.days
        )
        grant.rotateCredentials(
            presentedRenewalCredentialId = RenewalCredential.Id("renewal-1"),
            nextAccessCredential = rotatedAccessCredential,
            nextRenewalCredential = rotatedRenewalCredential,
            now = rotationTime
        )

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            grant.rotateCredentials(
                presentedRenewalCredentialId = RenewalCredential.Id("renewal-1"),
                nextAccessCredential = AccessCredential(
                    id = AccessCredential.Id("access-3"),
                    issuedAt = rotationTime,
                    willExpiresAt = rotationTime + 15.minutes
                ),
                nextRenewalCredential = RenewalCredential(
                    id = RenewalCredential.Id("renewal-3"),
                    issuedAt = rotationTime,
                    willExpiresAt = rotationTime + 7.days
                ),
                now = rotationTime + 1.seconds
            )
        }

        // then
        assertEquals("1-006-011", exception.code)
        assertEquals("access-2", grant.accessCredential.id.value)
        assertEquals(rotatedRenewalCredential, grant.renewalCredential)
    }

    private data class TestUserId(
        override val value: String
    ) : UserId

}
