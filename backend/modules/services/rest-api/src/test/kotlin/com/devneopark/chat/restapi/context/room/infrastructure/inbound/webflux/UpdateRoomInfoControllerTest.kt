package com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.room.application.exception.DuplicatedTitleException
import com.devneopark.chat.restapi.context.room.application.exception.RoomNotFoundException
import com.devneopark.chat.restapi.context.room.application.port.inbound.UpdateRoomInfoUseCase
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.controller.UpdateRoomInfoController
import com.devneopark.chat.restapi.context.room.infrastructure.inbound.webflux.specification.UpdateRoomInfoApi
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
@WebFluxTest(controllers = [ UpdateRoomInfoController::class ])
@Import(
    WebFluxSecurityConfig::class,
    TraceIdAssigningFilter::class,
    WebFluxGlobalExceptionAdvice::class,
    UpdateRoomInfoController::class
)
class UpdateRoomInfoControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @MockitoBean
    lateinit var updateRoomInfoUseCase: UpdateRoomInfoUseCase

    @Test
    fun `인증된 호스트가 채팅방 정보를 변경하면 성공 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomInfoApi.Request("Updated room", "NewPassword123")
        val command = UpdateRoomInfoUseCase.Command(
            roomId,
            userId,
            body.title!!,
            body.rawPassword
        )
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<UpdateRoomInfoApi.Response>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("0-000-000", responseBody.code)
        verify(updateRoomInfoUseCase).update(command)
    }

    @Test
    fun `비밀번호가 null이면 비밀번호 제거 명령을 전달한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomInfoApi.Request("Updated room", null)
        val command = UpdateRoomInfoUseCase.Command(
            roomId,
            userId,
            body.title!!,
            null
        )
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )

        // when
        webTestClient.put()
            .uri("/rooms/{roomId}", roomId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

        // then
        verify(updateRoomInfoUseCase).update(command)
    }

    @Test
    fun `인증되지 않은 사용자는 채팅방 정보를 변경할 수 없다`() {
        // when
        webTestClient.put()
            .uri("/rooms/{roomId}", "room-001")
            .bodyValue(UpdateRoomInfoApi.Request("Updated room", null))
            .exchange()
            .expectStatus()
            .isForbidden

        // then
        verifyNoInteractions(updateRoomInfoUseCase)
    }

    @Test
    fun `채팅방 정보 변경 요청 바디의 입력값이 검증을 통과하지 못하면 요청이 실패한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}", "room-001")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .bodyValue(UpdateRoomInfoApi.Request("", "a".repeat(33)))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<FieldBindingExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertNotNull(responseBody)
        assertEquals("3-001-003", responseBody.code)
        assertEquals(setOf("title", "rawPassword"), responseBody.fields.toSet())
        verifyNoInteractions(updateRoomInfoUseCase)
    }

    @Test
    fun `채팅방을 찾을 수 없으면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomInfoApi.Request("Updated room", null)
        val command = UpdateRoomInfoUseCase.Command(roomId, userId, body.title!!, body.rawPassword)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(updateRoomInfoUseCase.update(command))
            .willThrow(RoomNotFoundException())

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}", roomId)
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
    fun `채팅방 제목이 중복되면 응용 예외를 오류 응답으로 변환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        val userId = "user-001"
        val roomId = "room-001"
        val body = UpdateRoomInfoApi.Request("Duplicated room", null)
        val command = UpdateRoomInfoUseCase.Command(roomId, userId, body.title!!, body.rawPassword)
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result(userId, "access-jti-001")
        )
        given(updateRoomInfoUseCase.update(command))
            .willThrow(DuplicatedTitleException())

        // when
        val responseBody = webTestClient.put()
            .uri("/rooms/{roomId}", roomId)
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
