package com.devneopark.chat.lib.domain.user.reference

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
        val codes = definitions.map { it.code }
        val uniqueCodes = codes.distinct()

        // then
        assertEquals(codes.size, uniqueCodes.size)
    }

    @Test
    fun `given exception definitions when contract is checked then code and message match expected values`() {
        // then
        assertEquals("1-001-001", ExceptionDefinition.USER_ID_REQUIRED.code)
        assertEquals("User ID is required.", ExceptionDefinition.USER_ID_REQUIRED.message)

        assertEquals("1-001-002", ExceptionDefinition.INVALID_USER_PRINCIPAL.code)
        assertEquals("User principal is invalid.", ExceptionDefinition.INVALID_USER_PRINCIPAL.message)

        assertEquals("1-001-003", ExceptionDefinition.USER_PRINCIPAL_DUPLICATED.code)
        assertEquals("User principal is already in use.", ExceptionDefinition.USER_PRINCIPAL_DUPLICATED.message)

        assertEquals("1-001-004", ExceptionDefinition.INVALID_USER_PASSWORD.code)
        assertEquals("User password is invalid.", ExceptionDefinition.INVALID_USER_PASSWORD.message)

        assertEquals("1-001-005", ExceptionDefinition.USER_PASSWORD_REUSED.code)
        assertEquals("User password cannot be reused.", ExceptionDefinition.USER_PASSWORD_REUSED.message)

        assertEquals("1-001-006", ExceptionDefinition.INVALID_USER_DISPLAY_NAME.code)
        assertEquals("User display name is invalid.", ExceptionDefinition.INVALID_USER_DISPLAY_NAME.message)

        assertEquals("1-001-007", ExceptionDefinition.USER_NOT_FOUND.code)
        assertEquals("User was not found.", ExceptionDefinition.USER_NOT_FOUND.message)

        assertEquals("1-001-008", ExceptionDefinition.USER_PROFILE_REQUIRED.code)
        assertEquals("User profile is required.", ExceptionDefinition.USER_PROFILE_REQUIRED.message)

        assertEquals("1-001-009", ExceptionDefinition.USER_CREDENTIAL_REQUIRED.code)
        assertEquals("User credential is required.", ExceptionDefinition.USER_CREDENTIAL_REQUIRED.message)
    }

}
