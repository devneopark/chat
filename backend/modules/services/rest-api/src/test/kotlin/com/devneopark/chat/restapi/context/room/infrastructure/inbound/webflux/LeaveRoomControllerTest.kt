package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.RoomParticipantNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.LeaveRoomUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller.LeaveRoomController
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.LeaveRoomApi
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
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@WebFluxTest(controllers = [ LeaveRoomController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    LeaveRoomController::class
)
class LeaveRoomControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var leaveRoomUseCase: LeaveRoomUseCase

    @Test
    fun `인증된 사용자가 채팅방에서 퇴장하면 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.delete()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<LeaveRoomApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(leaveRoomUseCase).leave(LeaveRoomUseCase.Command(roomId, userId))
    }

    @Test
    fun `인증되지 않은 사용자는 채팅방에서 퇴장할 수 없다`() {
        // when
        webTestClient.delete()
            .uri("/rooms/{roomId}/participants", "room-001")
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(leaveRoomUseCase)
    }

    @Test
    fun `유효하지 않은 access credential이면 퇴장하지 않고 401을 반환한다`() = runTest {
        // given
        val serializedCredential = "invalid-access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willThrow(InvalidAccessCredentialException())

        // when
        webTestClient.delete()
            .uri("/rooms/{roomId}/participants", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isUnauthorized

        // then
        verifyNoInteractions(leaveRoomUseCase)
    }

    @Test
    fun `영속성 장애는 Service Unavailable 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(leaveRoomUseCase.leave(LeaveRoomUseCase.Command(roomId, userId)))
            .willThrow(DataAccessResourceFailureException("database unavailable"))

        // when
        val responseBody = webTestClient.delete()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
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
    fun `채팅방을 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(leaveRoomUseCase.leave(LeaveRoomUseCase.Command(roomId, userId)))
            .willThrow(RoomNotFoundException())

        // when
        val responseBody = webTestClient.delete()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals(RoomNotFoundException().code, responseBody.code)
        assertEquals(RoomNotFoundException().message, responseBody.message)
    }

    @Test
    fun `활성 참여자를 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val userId = "user-001"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(leaveRoomUseCase.leave(LeaveRoomUseCase.Command(roomId, userId)))
            .willThrow(RoomParticipantNotFoundException())

        // when
        val responseBody = webTestClient.delete()
            .uri("/rooms/{roomId}/participants", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals(RoomParticipantNotFoundException().code, responseBody.code)
        assertEquals(RoomParticipantNotFoundException().message, responseBody.message)
    }

}
