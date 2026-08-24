package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http

import com.devneopark.chat.restapi.context.iam.application.exception.IamContextException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.GrantAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.controller.LoginUserController
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.http.specification.LoginUserApi
import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.ExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.FieldBindingExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.http.TraceIdAssigningFilter
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
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant as KotlinInstant
import kotlin.time.toJavaInstant
import kotlin.test.assertTrue

@ActiveProfiles("test")
@WebFluxTest(controllers = [ LoginUserController::class ])
@Import(TraceIdAssigningFilter::class, LoginUserController::class)
class LoginUserControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var grantAuthenticationUseCase: GrantAuthenticationUseCase

    @MockitoBean
    lateinit var clock: Clock

    @Test
    fun `로그인하면 access token과 만료시각을 반환하고 renewal credential을 쿠키로 설정한다`() = runTest {
        // given
        val body = LoginUserApi.Request(
            "principal",
            "RawP@ssword123"
        )
        val command = GrantAuthenticationUseCase.Command(
            body.principal!!,
            body.rawPassword!!
        )
        val now = Instant.parse("2026-08-11T00:00:00Z")
        val accessExpiresAt = KotlinInstant.parse("2026-08-11T00:15:00Z")
        val renewalExpiresAt = accessExpiresAt + 7.days
        BDDMockito.given(clock.instant())
            .willReturn(now)
        BDDMockito.given(grantAuthenticationUseCase.grant(command))
            .willReturn(
                GrantAuthenticationUseCase.Result(
                    "user-001",
                    GrantAuthenticationUseCase.Credential(
                        "access-token",
                        accessExpiresAt
                    ),
                    GrantAuthenticationUseCase.Credential(
                        "renewal-token",
                        renewalExpiresAt
                    )
                )
            )

        // when
        val responseBody = webTestClient.post()
            .uri("/authentications")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .value(HttpHeaders.SET_COOKIE) {
                assertTrue(it.contains("refreshToken=renewal-token"))
            }
            .expectBody<LoginUserApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals("0-000-000", responseBody.code)
        Assertions.assertEquals("access-token", responseBody.accessToken)
        Assertions.assertEquals(accessExpiresAt.toJavaInstant(), responseBody.expiresAt)
    }

    @Test
    fun `로그인 요청 바디의 입력값이 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        // given
        val body = LoginUserApi.Request("", "")

        // when
        val responseBody = webTestClient.post()
            .uri("/authentications")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals("3-001-003", responseBody.code)
        Assertions.assertTrue {
            val expectedFields = listOf("principal", "rawPassword")
            responseBody.fields.containsAll(expectedFields)
        }
    }

    @Test
    fun `인증에 실패하면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val body = LoginUserApi.Request(
            "principal",
            "RawP@ssword123"
        )
        val command = GrantAuthenticationUseCase.Command(
            body.principal!!,
            body.rawPassword!!
        )
        BDDMockito.given(grantAuthenticationUseCase.grant(command))
            .willThrow(IamContextException("2-001-003", "Wrong password."))

        // when
        val responseBody = webTestClient.post()
            .uri("/authentications")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        Assertions.assertNotNull(responseBody)
        Assertions.assertEquals("2-001-003", responseBody.code)
        Assertions.assertEquals("Wrong password.", responseBody.message)
    }

}
