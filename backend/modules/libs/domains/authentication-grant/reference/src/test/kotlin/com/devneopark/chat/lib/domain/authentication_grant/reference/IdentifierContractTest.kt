package com.devneopark.chat.lib.domain.authentication_grant.reference

import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifierContractTest {

    @Test
    fun `given authentication grant id implementation when value is read then original value is exposed`() {
        // given
        val value = "grant-1"
        val id: AuthenticationGrantId = TestAuthenticationGrantId(value)

        // when
        val actualValue = id.value

        // then
        assertEquals(value, actualValue)
    }

    @Test
    fun `given access credential id implementation when value is read then original value is exposed`() {
        // given
        val value = "access-1"
        val id: AccessCredentialId = TestAccessCredentialId(value)

        // when
        val actualValue = id.value

        // then
        assertEquals(value, actualValue)
    }

    @Test
    fun `given renewal credential id implementation when value is read then original value is exposed`() {
        // given
        val value = "credential-1"
        val id: RenewalCredentialId = TestRenewalCredentialId(value)

        // when
        val actualValue = id.value

        // then
        assertEquals(value, actualValue)
    }

    private data class TestAuthenticationGrantId(
        override val value: String
    ) : AuthenticationGrantId

    private data class TestAccessCredentialId(
        override val value: String
    ) : AccessCredentialId

    private data class TestRenewalCredentialId(
        override val value: String
    ) : RenewalCredentialId

}
