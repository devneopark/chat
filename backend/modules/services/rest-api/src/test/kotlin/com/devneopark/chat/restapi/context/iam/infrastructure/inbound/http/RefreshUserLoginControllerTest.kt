package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http

import com.devneopark.chat.restapi.context.iam.application.exception.ExceptionDefinition
import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.RenewalAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.controller.RefreshUserLoginController
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.specification.RefreshUserLoginApi
import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.ExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.TraceIdAssigningFilter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaInstant

@ActiveProfiles("test")
@WebFluxTest(controllers = [ RefreshUserLoginController::class ])
@Import(TraceIdAssigningFilter::class, RefreshUserLoginController::class)
class RefreshUserLoginControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var renewalAuthenticationUseCase: RenewalAuthenticationUseCase

    @MockitoBean
    lateinit var clock: Clock

    @Test
    fun `refresh token으로 access token을 재발급하고 새로운 renewal credential을 쿠키로 설정한다`() = runTest {
        // given
        val refreshToken = "renewal-token"
        val command = RenewalAuthenticationUseCase.Command(refreshToken)
        val now = Instant.parse("2026-08-11T00:00:00Z")
        val accessExpiresAt = kotlin.time.Instant.parse("2026-08-11T00:15:00Z")
        val renewalExpiresAt = accessExpiresAt + 7.minutes
        given(clock.instant())
            .willReturn(now)
        given(renewalAuthenticationUseCase.renewal(command))
            .willReturn(
                RenewalAuthenticationUseCase.Result(
                    "user-001",
                    RenewalAuthenticationUseCase.Credential(
                        "access-token",
                        accessExpiresAt
                    ),
                    RenewalAuthenticationUseCase.Credential(
                        "renewal-token",
                        renewalExpiresAt
                    )
                )
            )

        // when
        val responseBody = webTestClient.put()
            .uri("/authentications")
            .cookie("SRTID", refreshToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .value(HttpHeaders.SET_COOKIE) {
                Assertions.assertTrue(it.contains("SRTID=renewal-token"))
            }
            .expectBody<RefreshUserLoginApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals("0-000-000", responseBody.code)
        Assertions.assertEquals("access-token", responseBody.accessToken)
        Assertions.assertEquals(accessExpiresAt.toJavaInstant(), responseBody.expiresAt)
    }

    @Test
    fun `유효하지 않은 refresh token이면 오류 응답을 반환한다`() = runTest {
        // given
        val refreshToken = "invalid-renewal-token"
        val command = RenewalAuthenticationUseCase.Command(refreshToken)
        given(renewalAuthenticationUseCase.renewal(command))
            .willThrow(
                IamContextException(
                    ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.code,
                    ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.message
                )
            )

        // when
        val responseBody = webTestClient.put()
            .uri("/authentications")
            .cookie("SRTID", refreshToken)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals(ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.code, responseBody.code)
        Assertions.assertEquals(ExceptionDefinition.INVALID_RENEWAL_CREDENTIAL.message, responseBody.message)
    }

}
