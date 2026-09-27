package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.room.application.exception.RoomFullException
import com.devneopark.chat.restapi.context.room.application.port.inbound.JoinRoomUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller.JoinRoomController
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.JoinRoomApi
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
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@WebFluxTest(controllers = [ JoinRoomController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    JoinRoomController::class
)
class JoinRoomControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var joinRoomUseCase: JoinRoomUseCase

    @Test
    fun `인증된 사용자가 채팅방에 입장하면 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val userId = "user-001"
        val body = JoinRoomApi.Request("RoomPassword1")
        val command = JoinRoomUseCase.Command(roomId, userId, body.rawPassword)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<JoinRoomApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(joinRoomUseCase).join(command)
    }

    @Test
    fun `인증되지 않은 사용자는 채팅방에 입장할 수 없다`() {
        // when
        webTestClient.post()
            .uri("/rooms/{roomId}/participants", "room-001")
            .bodyValue(JoinRoomApi.Request())
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(joinRoomUseCase)
    }

    @Test
    fun `채팅방 입장 요청 바디의 비밀번호가 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/participants", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(JoinRoomApi.Request("a".repeat(33)))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        assertEquals(setOf("rawPassword"), responseBody.fields.toSet())
        verifyNoInteractions(joinRoomUseCase)
    }

    @Test
    fun `채팅방에 빈 슬롯이 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val command = JoinRoomUseCase.Command(roomId, userId, null)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(joinRoomUseCase.join(command)).willThrow(RoomFullException())

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(JoinRoomApi.Request())
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals(RoomFullException().code, responseBody.code)
        assertEquals(RoomFullException().message, responseBody.message)
    }

    @Test
    fun `참여자 유니크 제약 위반은 Conflict 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val command = JoinRoomUseCase.Command(roomId, userId, null)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(joinRoomUseCase.join(command))
            .willThrow(DuplicateKeyException("participant room and user already exists"))

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(JoinRoomApi.Request())
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-006", responseBody.code)
        assertEquals("Conflict.", responseBody.message)
    }

    @Test
    fun `영속성 장애는 Service Unavailable 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val command = JoinRoomUseCase.Command(roomId, userId, null)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(joinRoomUseCase.join(command))
            .willThrow(DataAccessResourceFailureException("database unavailable"))

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(JoinRoomApi.Request())
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-007", responseBody.code)
        assertEquals("Service unavailable.", responseBody.message)
    }

    @Test
    fun `인증된 사용자가 요청 바디 없이 입장하면 요청 바디 누락 오류를 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/participants", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-004", responseBody.code)
        assertEquals("Request body missing.", responseBody.message)
        verifyNoInteractions(joinRoomUseCase)
    }

}
