package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RegisterUserUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.controller.RegisterUserController
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification.RegisterUserApi
import com.devneopark.chat.restapi.framework.advice.WebFluxGlobalExceptionAdvice
import com.devneopark.chat.restapi.framework.config.WebFluxSecurityConfig
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.FieldBindingExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.TraceIdAssigningFilter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.mockito.Mockito.verifyNoInteractions

@ActiveProfiles("test")
@WebFluxTest(controllers = [ RegisterUserController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    RegisterUserController::class
)
class RegisterUserControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var registerUserUseCase: RegisterUserUseCase

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @Test
    fun `회원가입하면 생성된 유저의 식별자를 반환한다`() = runTest {
        val body = RegisterUserApi.Request("principal", "P@ssword123")
        val command = RegisterUserUseCase.Command(
            body.principal!!,
            body.rawPassword!!,
            body.displayName ?: body.principal
        )
        BDDMockito.given(registerUserUseCase.register(command))
            .willReturn(RegisterUserUseCase.Result("user-001"))

        val responseBody = webTestClient.post()
            .uri("/users")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody<RegisterUserApi.Response>()
            .returnResult()
            .responseBody!!

        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals("0-000-000", responseBody.code)
        Assertions.assertEquals("user-001", responseBody.userId)
    }

    @Test
    fun `인증된 사용자는 회원가입할 수 없다`() = runTest {
        // given
        val serializedCredential = "access-token"
        BDDMockito.given(
            authenticateAccessCredentialUseCase.authenticate(
                AuthenticateAccessCredentialUseCase.Command(serializedCredential)
            )
        ).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )
        val body = RegisterUserApi.Request("principal", "P@ssword123")

        // when
        webTestClient.post()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(registerUserUseCase)
    }

    @Test
    fun `바디의 입력값이 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        val body = RegisterUserApi.Request("", "", "")

        val responseBody = webTestClient.post()
            .uri("/users")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals("3-001-003", responseBody.code)
        Assertions.assertTrue {
            val expectedFields = listOf("principal", "rawPassword", "displayName")
            responseBody.fields.containsAll(expectedFields)
        }
    }

}
