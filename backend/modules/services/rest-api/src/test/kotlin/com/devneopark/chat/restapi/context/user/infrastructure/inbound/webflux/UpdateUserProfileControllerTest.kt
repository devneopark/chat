package com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.user.application.exception.UserNotFoundException
import com.devneopark.chat.restapi.context.user.application.port.inbound.UpdateUserProfileUseCase
import com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.controller.UpdateUserProfileController
import com.devneopark.chat.restapi.context.user.infrastructure.inbound.webflux.specification.UpdateUserProfileApi
import com.devneopark.chat.restapi.framework.advice.WebFluxGlobalExceptionAdvice
import com.devneopark.chat.restapi.framework.config.WebFluxSecurityConfig
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.FieldBindingExceptionResponse
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
@WebFluxTest(controllers = [ UpdateUserProfileController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    UpdateUserProfileController::class
)
class UpdateUserProfileControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase

    @Test
    fun `인증된 사용자의 프로필을 수정하고 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val body = UpdateUserProfileApi.Request("Updated Neo")
        val command = UpdateUserProfileUseCase.Command(userId, body.displayName!!)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/users/profiles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody<UpdateUserProfileApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(updateUserProfileUseCase).update(command)
    }

    @Test
    fun `인증되지 않은 사용자는 프로필을 수정할 수 없다`() {
        // when
        webTestClient.put()
            .uri("/users/profiles")
            .bodyValue(UpdateUserProfileApi.Request("Updated Neo"))
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(updateUserProfileUseCase)
    }

    @Test
    fun `프로필 수정 요청 바디의 입력값이 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/users/profiles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(UpdateUserProfileApi.Request(""))
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        org.junit.jupiter.api.Assertions.assertTrue(responseBody.fields.contains("displayName"))
        verifyNoInteractions(updateUserProfileUseCase)
    }

    @Test
    fun `사용자를 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val body = UpdateUserProfileApi.Request("Updated Neo")
        val command = UpdateUserProfileUseCase.Command(userId, body.displayName!!)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(updateUserProfileUseCase.update(command))
            .willThrow(UserNotFoundException())

        // when
        val responseBody = webTestClient.put()
            .uri("/users/profiles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
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
