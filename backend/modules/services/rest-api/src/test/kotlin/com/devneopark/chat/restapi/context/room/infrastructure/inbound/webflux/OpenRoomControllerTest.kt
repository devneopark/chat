package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.room.application.exception.DuplicatedTitleException
import com.devneopark.chat.restapi.context.room.application.port.inbound.OpenRoomUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller.OpenRoomController
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.OpenRoomApi
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
@WebFluxTest(controllers = [ OpenRoomController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    OpenRoomController::class
)
class OpenRoomControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var openRoomUseCase: OpenRoomUseCase

    @Test
    fun `인증된 사용자가 채팅방을 개설하면 생성된 방 식별자를 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val body = OpenRoomApi.Request("Open room", "RawPassword123", 10)
        val command = OpenRoomUseCase.Command(
            body.title!!,
            body.rawPassword,
            body.capacity!!,
            userId
        )
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(openRoomUseCase.open(command))
            .willReturn(OpenRoomUseCase.Result("room-001"))

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<OpenRoomApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        assertEquals("room-001", responseBody.roomId)
        verify(openRoomUseCase).open(command)
    }

    @Test
    fun `인증되지 않은 사용자는 채팅방을 개설할 수 없다`() {
        // when
        webTestClient.post()
            .uri("/rooms")
            .bodyValue(OpenRoomApi.Request("Open room", null, 10))
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(openRoomUseCase)
    }

    @Test
    fun `채팅방 개설 요청 바디의 입력값이 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(OpenRoomApi.Request("", "a".repeat(33), null))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        assertEquals(
            setOf("title", "rawPassword", "capacity"),
            responseBody.fields.toSet()
        )
        verifyNoInteractions(openRoomUseCase)
    }

    @Test
    fun `채팅방 제목이 중복되면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val body = OpenRoomApi.Request("Duplicated room", null, 10)
        val command = OpenRoomUseCase.Command(
            body.title!!,
            body.rawPassword,
            body.capacity!!,
            userId
        )
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(openRoomUseCase.open(command))
            .willThrow(DuplicatedTitleException())

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals(DuplicatedTitleException().code, responseBody.code)
        assertEquals(DuplicatedTitleException().message, responseBody.message)
    }

}
