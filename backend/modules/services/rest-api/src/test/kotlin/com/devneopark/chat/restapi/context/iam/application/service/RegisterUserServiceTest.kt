package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.lib.domain.user.service.UserCredentialValidator
import com.devneopark.chat.lib.domain.user.service.UserProfileValidator
import com.devneopark.chat.lib.shared.domain.exception.DomainRuleViolationException
import com.devneopark.chat.libs.shared.application.identifier.IdGenerator
import com.devneopark.chat.restapi.context.iam.application.exception.DuplicatedPrincipalException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RegisterUserUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.PasswordHasher
import com.devneopark.chat.restapi.context.iam.application.port.outbound.IamUserRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.devneopark.chat.lib.domain.user.reference.ExceptionDefinition as UserExceptionDefinition

@ExtendWith(MockitoExtension::class)
class RegisterUserServiceTest {

    @Mock
    lateinit var iamUserRepositoryPort: IamUserRepositoryPort

    @Mock
    lateinit var idGenerator: IdGenerator

    @Mock
    lateinit var userCredentialValidator: UserCredentialValidator

    @Mock
    lateinit var userProfileValidator: UserProfileValidator

    @Mock
    lateinit var passwordHasher: PasswordHasher

    @InjectMocks
    lateinit var registerUserService: RegisterUserService

    @Test
    fun `회원가입하면 비밀번호를 해시하고 생성된 사용자의 식별자를 반환한다`() = runTest {
        // given
        val command = RegisterUserUseCase.Command(
            "principal",
            "raw-password",
            "display name"
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(anyString())
        willDoNothing()
            .given(userCredentialValidator)
            .validatePassword(anyString())
        willDoNothing()
            .given(userProfileValidator)
            .validateDisplayName(anyString())

        given(iamUserRepositoryPort.existsByPrincipal(anyString()))
            .willReturn(false)

        val idValue = "generated-id-value"
        val hashedPassword = "hashed-password"
        given(idGenerator.generate())
            .willReturn(idValue)
        given(passwordHasher.hash(anyString()))
            .willReturn(hashedPassword)

        // when
        val registrationResult = registerUserService.register(command)

        // then
        assertEquals(idValue, registrationResult.userId)
    }

    @Test
    fun `principal 입력값 검사를 통과하지 못하면 예외를 던진다`() = runTest {
        // given
        val principal = "pr!nc!pa/"
        val command = RegisterUserUseCase.Command(
            principal,
            "RawP@assword",
            "display name"
        )
        given(userCredentialValidator.validatePrincipal(anyString()))
            .willThrow(DomainRuleViolationException(
                UserExceptionDefinition.INVALID_USER_PRINCIPAL.code,
                UserExceptionDefinition.INVALID_USER_PRINCIPAL.message
            ))

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            registerUserService.register(command)
        }

        // then
        assertEquals(UserExceptionDefinition.INVALID_USER_PRINCIPAL.code, exception.code)
        assertEquals(UserExceptionDefinition.INVALID_USER_PRINCIPAL.message, exception.message)
        verify(userCredentialValidator)
            .validatePrincipal(principal)
        verify(userCredentialValidator, never())
            .validatePassword(anyString())
        verifyNoInteractions(
            userProfileValidator,
            iamUserRepositoryPort,
            idGenerator,
            passwordHasher
        )
    }

    @Test
    fun `password 입력값 검사를 통과하지 못하면 예외를 던진다`() = runTest {
        // given
        val principal = "principal"
        val rawPassword = "password"
        val command = RegisterUserUseCase.Command(
            principal,
            rawPassword,
            "display name"
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(anyString())
        given(userCredentialValidator.validatePassword(rawPassword))
            .willThrow(DomainRuleViolationException(
                UserExceptionDefinition.INVALID_USER_PASSWORD.code,
                UserExceptionDefinition.INVALID_USER_PASSWORD.message
            ))

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            registerUserService.register(command)
        }

        // then
        assertEquals(UserExceptionDefinition.INVALID_USER_PASSWORD.code, exception.code)
        assertEquals(UserExceptionDefinition.INVALID_USER_PASSWORD.message, exception.message)
        verify(userCredentialValidator)
            .validatePassword(command.rawPassword)
        verifyNoInteractions(
            userProfileValidator,
            iamUserRepositoryPort,
            idGenerator,
            passwordHasher
        )
    }

    @Test
    fun `displayName 입력값 검사를 통과하지 못하면 예외를 던진다`() = runTest {
        // given
        val principal = "principal"
        val rawPassword = "password"
        val displayName = ""
        val command = RegisterUserUseCase.Command(
            principal,
            rawPassword,
            displayName
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(anyString())
        willDoNothing()
            .given(userCredentialValidator)
            .validatePassword(anyString())
        given(userProfileValidator.validateDisplayName(displayName))
            .willThrow(DomainRuleViolationException(
                UserExceptionDefinition.INVALID_USER_DISPLAY_NAME.code,
                UserExceptionDefinition.INVALID_USER_DISPLAY_NAME.message
            ))

        // when
        val exception = assertFailsWith<DomainRuleViolationException> {
            registerUserService.register(command)
        }

        // then
        assertEquals(UserExceptionDefinition.INVALID_USER_DISPLAY_NAME.code, exception.code)
        assertEquals(UserExceptionDefinition.INVALID_USER_DISPLAY_NAME.message, exception.message)
        verify(userProfileValidator)
            .validateDisplayName(command.displayName)
        verifyNoInteractions(
            iamUserRepositoryPort,
            idGenerator,
            passwordHasher
        )
    }

    @Test
    fun `이미 존재하는 principal이라면 예외를 던진다`() = runTest {
        // given
        val principal = "principal"
        val rawPassword = "password"
        val displayName = ""
        val command = RegisterUserUseCase.Command(
            principal,
            rawPassword,
            displayName
        )
        willDoNothing()
            .given(userCredentialValidator)
            .validatePrincipal(anyString())
        willDoNothing()
            .given(userCredentialValidator)
            .validatePassword(anyString())
        willDoNothing()
            .given(userProfileValidator)
            .validateDisplayName(anyString())
        given(iamUserRepositoryPort.existsByPrincipal(anyString()))
            .willReturn(true)

        // when
        val exception = assertFailsWith<DuplicatedPrincipalException> {
            registerUserService.register(command)
        }

        // then
        assertEquals("2-001-001", exception.code)
        assertEquals("Principal duplicated.", exception.message)
        verify(iamUserRepositoryPort, only())
            .existsByPrincipal(anyString())
        verifyNoInteractions(
            idGenerator,
            passwordHasher
        )
    }

}
