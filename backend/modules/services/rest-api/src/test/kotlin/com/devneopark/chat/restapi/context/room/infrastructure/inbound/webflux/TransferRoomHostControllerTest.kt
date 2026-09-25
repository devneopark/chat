package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.exception.RoomParticipantNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.TransferRoomHostUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller.TransferRoomHostController
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.TransferRoomHostApi
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
@WebFluxTest(controllers = [ TransferRoomHostController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    TransferRoomHostController::class
)
class TransferRoomHostControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var transferRoomHostUseCase: TransferRoomHostUseCase

    @Test
    fun `인증된 호스트가 호스트 권한을 이전하면 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val requesterUserId = "user-001"
        val targetUserId = "user-002"
        val body = TransferRoomHostApi.Request(targetUserId)
        val command = TransferRoomHostUseCase.Command(roomId, requesterUserId, targetUserId)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(requesterUserId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/host", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TransferRoomHostApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(transferRoomHostUseCase).transfer(command)
    }

    @Test
    fun `인증되지 않은 사용자는 호스트를 변경할 수 없다`() {
        // when
        webTestClient.post()
            .uri("/rooms/{roomId}/host", "room-001")
            .bodyValue(TransferRoomHostApi.Request("user-002"))
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(transferRoomHostUseCase)
    }

    @Test
    fun `호스트 변경 요청 바디의 대상 사용자 식별자가 비어 있으면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/host", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(TransferRoomHostApi.Request())
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        assertEquals(setOf("targetUserId"), responseBody.fields.toSet())
        verifyNoInteractions(transferRoomHostUseCase)
    }

    @Test
    fun `채팅방을 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val requesterUserId = "user-001"
        val targetUserId = "user-002"
        val body = TransferRoomHostApi.Request(targetUserId)
        val command = TransferRoomHostUseCase.Command(roomId, requesterUserId, targetUserId)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(requesterUserId, "access-jti-001")
        )
        given(transferRoomHostUseCase.transfer(command))
            .willThrow(RoomNotFoundException())

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/host", roomId)
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
        assertEquals(RoomNotFoundException().code, responseBody.code)
        assertEquals(RoomNotFoundException().message, responseBody.message)
    }

    @Test
    fun `활성 게스트를 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val roomId = "room-001"
        val requesterUserId = "user-001"
        val targetUserId = "user-002"
        val body = TransferRoomHostApi.Request(targetUserId)
        val command = TransferRoomHostUseCase.Command(roomId, requesterUserId, targetUserId)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(requesterUserId, "access-jti-001")
        )
        given(transferRoomHostUseCase.transfer(command))
            .willThrow(RoomParticipantNotFoundException())

        // when
        val responseBody = webTestClient.post()
            .uri("/rooms/{roomId}/host", roomId)
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
        assertEquals(RoomParticipantNotFoundException().code, responseBody.code)
        assertEquals(RoomParticipantNotFoundException().message, responseBody.message)
    }

}
