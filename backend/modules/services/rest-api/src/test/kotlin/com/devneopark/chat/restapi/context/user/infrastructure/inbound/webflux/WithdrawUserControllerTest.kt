package com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import com.devneopark.chat.restapi.context.user.application.exception.UserHasActiveRoomException
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.WithdrawUserUseCase
import com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.controller.WithdrawUserController
import com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.specification.WithdrawUserApi
import com.devneopark.chat.restapi.framework.advice.WebFluxGlobalExceptionAdvice
import com.devneopark.chat.restapi.framework.config.WebFluxSecurityConfig
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.TraceIdAssigningFilter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@WebFluxTest(controllers = [ WithdrawUserController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    WithdrawUserController::class
)
class WithdrawUserControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var withdrawUserUseCase: WithdrawUserUseCase

    @Test
    fun `인증된 사용자를 탈퇴 처리하고 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.delete()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody<WithdrawUserApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(withdrawUserUseCase).withdraw(
            WithdrawUserUseCase.Command(userId)
        )
    }

    @Test
    fun `인증되지 않은 사용자는 탈퇴할 수 없다`() {
        // when
        webTestClient.delete()
            .uri("/users")
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(authenticateAccessCredentialUseCase, withdrawUserUseCase)
    }

    @Test
    fun `유효하지 않은 access credential이면 탈퇴하지 않고 401을 반환한다`() = runTest {
        // given
        val serializedCredential = "invalid-access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willThrow(InvalidAccessCredentialException())

        // when
        webTestClient.delete()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isUnauthorized

        // then
        verifyNoInteractions(withdrawUserUseCase)
    }

    @Test
    fun `활성 채팅방에 참여 중이면 오류 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(withdrawUserUseCase.withdraw(WithdrawUserUseCase.Command(userId)))
            .willThrow(UserHasActiveRoomException())

        // when
        val responseBody = webTestClient.delete()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals(UserHasActiveRoomException().code, responseBody.code)
        assertEquals(UserHasActiveRoomException().message, responseBody.message)
    }

    @Test
    fun `사용자를 찾을 수 없으면 오류 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(withdrawUserUseCase.withdraw(WithdrawUserUseCase.Command(userId)))
            .willThrow(UserNotFoundException())

        // when
        val responseBody = webTestClient.delete()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals(UserNotFoundException().code, responseBody.code)
        assertEquals(UserNotFoundException().message, responseBody.message)
    }

}
