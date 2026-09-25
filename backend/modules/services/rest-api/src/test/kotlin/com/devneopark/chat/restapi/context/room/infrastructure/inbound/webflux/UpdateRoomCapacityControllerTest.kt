package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.room.application.exception.CapacityReductionUnavailableException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomCapacityUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller.UpdateRoomCapacityController
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.UpdateRoomCapacityApi
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
@WebFluxTest(controllers = [ UpdateRoomCapacityController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    UpdateRoomCapacityController::class
)
class UpdateRoomCapacityControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var updateRoomCapacityUseCase: UpdateRoomCapacityUseCase

    @Test
    fun `인증된 호스트가 채팅방 정원을 변경하면 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomCapacityApi.Request(10)
        val command = UpdateRoomCapacityUseCase.Command(roomId, userId, body.capacity!!)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}/capacity", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<UpdateRoomCapacityApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(updateRoomCapacityUseCase).update(command)
    }

    @Test
    fun `인증되지 않은 사용자는 채팅방 정원을 변경할 수 없다`() {
        // when
        webTestClient.put()
            .uri("/rooms/{roomId}/capacity", "room-001")
            .bodyValue(UpdateRoomCapacityApi.Request(10))
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(updateRoomCapacityUseCase)
    }

    @Test
    fun `채팅방 정원 변경 요청 바디의 입력값이 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}/capacity", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(UpdateRoomCapacityApi.Request())
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        assertEquals(setOf("capacity"), responseBody.fields.toSet())
        verifyNoInteractions(updateRoomCapacityUseCase)
    }

    @Test
    fun `채팅방 정원이 1 이하이면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}/capacity", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(UpdateRoomCapacityApi.Request(1))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        assertEquals(setOf("capacity"), responseBody.fields.toSet())
        verifyNoInteractions(updateRoomCapacityUseCase)
    }

    @Test
    fun `채팅방을 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomCapacityApi.Request(10)
        val command = UpdateRoomCapacityUseCase.Command(roomId, userId, body.capacity!!)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(updateRoomCapacityUseCase.update(command))
            .willThrow(RoomNotFoundException())

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}/capacity", roomId)
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
    fun `빈 슬롯을 확보할 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomCapacityApi.Request(3)
        val command = UpdateRoomCapacityUseCase.Command(roomId, userId, body.capacity!!)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(updateRoomCapacityUseCase.update(command))
            .willThrow(CapacityReductionUnavailableException())

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}/capacity", roomId)
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
        assertEquals(CapacityReductionUnavailableException().code, responseBody.code)
        assertEquals(CapacityReductionUnavailableException().message, responseBody.message)
    }

}
