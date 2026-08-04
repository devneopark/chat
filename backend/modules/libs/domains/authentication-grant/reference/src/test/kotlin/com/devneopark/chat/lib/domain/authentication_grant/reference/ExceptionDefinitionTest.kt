package com.devneopark.chat.lib.domain.authentication_grant.reference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionDefinitionTest {

    @Test
    fun `given exception definitions when inspected then code and message are not blank`() {
        // given
        val definitions = ExceptionDefinition.entries

        // when
        val blankDefinitions = definitions.filter { definition ->
            definition.code.isBlank() || definition.message.isBlank()
        }

        // then
        assertTrue(blankDefinitions.isEmpty())
    }

    @Test
    fun `given exception definitions when codes are collected then codes are unique`() {
        // given
        val definitions = ExceptionDefinition.entries

        // when
        val codes = definitions.map { definition -> definition.code }

        // then
        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun `given authentication grant exception definitions when contract is checked then values are preserved`() {
        // then
        assertEquals("1-006-001", ExceptionDefinition.AUTHENTICATION_GRANT_ID_REQUIRED.code)
        assertEquals("Authentication grant ID is required.", ExceptionDefinition.AUTHENTICATION_GRANT_ID_REQUIRED.message)

        assertEquals("1-006-005", ExceptionDefinition.ACCESS_CREDENTIAL_ID_REQUIRED.code)
        assertEquals("Access credential ID is required.", ExceptionDefinition.ACCESS_CREDENTIAL_ID_REQUIRED.message)

        assertEquals("1-006-004", ExceptionDefinition.RENEWAL_CREDENTIAL_ID_REQUIRED.code)
        assertEquals("Renewal credential ID is required.", ExceptionDefinition.RENEWAL_CREDENTIAL_ID_REQUIRED.message)

        assertEquals("1-006-017", ExceptionDefinition.CREDENTIAL_ISSUED_AT_INVALID.code)
        assertEquals("Credential issued-at is invalid.", ExceptionDefinition.CREDENTIAL_ISSUED_AT_INVALID.message)

        assertEquals("1-006-018", ExceptionDefinition.INVALID_ACCESS_CREDENTIAL_EXPIRATION.code)
        assertEquals("Access credential expiration is invalid.", ExceptionDefinition.INVALID_ACCESS_CREDENTIAL_EXPIRATION.message)

        assertEquals("1-006-020", ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL_EXPIRATION.code)
        assertEquals("Renewal credential expiration is invalid.", ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL_EXPIRATION.message)

        assertEquals("1-006-010", ExceptionDefinition.RENEWAL_CREDENTIAL_NOT_USABLE.code)
        assertEquals("Renewal credential is not usable.", ExceptionDefinition.RENEWAL_CREDENTIAL_NOT_USABLE.message)

        assertEquals("1-006-011", ExceptionDefinition.RENEWAL_CREDENTIAL_MISMATCH.code)
        assertEquals("Renewal credential does not match the current credential.", ExceptionDefinition.RENEWAL_CREDENTIAL_MISMATCH.message)
    }

}
