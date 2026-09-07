package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RevokeAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.controller.RevokeAuthenticationController
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification.RevokeAuthenticationApi
import com.devneopark.chat.restapi.framework.advice.WebFluxGlobalExceptionAdvice
import com.devneopark.chat.restapi.framework.config.WebFluxSecurityConfig
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.TraceIdAssigningFilter
import kotlinx.coroutines.test.runTest
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
@WebFluxTest(controllers = [ RevokeAuthenticationController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    RevokeAuthenticationController::class
)
class RevokeAuthenticationControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var revokeAuthenticationUseCase: RevokeAuthenticationUseCase

    @Test
    fun `인증된 access credential의 jti로 인증정보를 삭제하고 200을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val jti = "access-jti-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", jti)
        )

        // when
        webTestClient.delete()
            .uri("/authentications")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<RevokeAuthenticationApi.Response>()
            .consumeWith { response ->
                check(response.responseBody?.code == "0-000-000")
            }

        // then
        verify(revokeAuthenticationUseCase).revoke(
            RevokeAuthenticationUseCase.Command(jti)
        )
    }

    @Test
    fun `인증되지 않은 요청은 로그아웃할 수 없다`() {
        // when
        webTestClient.delete()
            .uri("/authentications")
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(revokeAuthenticationUseCase)
    }

    @Test
    fun `유효하지 않은 access credential이면 로그아웃하지 않고 401을 반환한다`() = runTest {
        // given
        val serializedCredential = "invalid-access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willThrow(InvalidAccessCredentialException())

        // when
        webTestClient.delete()
            .uri("/authentications")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isUnauthorized

        // then
        verifyNoInteractions(revokeAuthenticationUseCase)
    }

}
