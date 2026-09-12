package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import com.devneopark.chat.restapi.framework.config.WebFluxSecurityConfig
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.ExceptionResponse
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.WebFluxErrorResponses
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import org.mockito.BDDMockito.given

@ActiveProfiles("test")
@WebFluxTest(controllers = [ WebFluxSecurityTestController::class ])
@Import(
    WebFluxSecurityConfig::class
)
class WebFluxSecurityConfigTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @MockitoBean
    lateinit var authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

    @Test
    fun `기본 reactive user details service 자동 구성을 사용하지 않는다`() {
        // then
        assertEquals(0, applicationContext.getBeansOfType(ReactiveUserDetailsService::class.java).size)
    }

    @Test
    fun `인증되지 않은 PreAuthorize 보호 자원 요청은 403을 반환한다`() {
        // when
        val response = webTestClient.get()
            .uri("/protected")
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertEquals(WebFluxErrorResponses.forbidden().code, response.code)
        assertEquals(WebFluxErrorResponses.forbidden().message, response.message)
    }

    @Test
    fun `isAnonymous 자원은 인증되지 않은 사용자에게 허용된다`() {
        webTestClient.get()
            .uri("/anonymous-only")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `isAnonymous 자원은 인증된 사용자에게 거부된다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        webTestClient.get()
            .uri("/anonymous-only")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `permitAll 자원은 인증되지 않은 사용자에게 허용된다`() {
        webTestClient.get()
            .uri("/all-users")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `permitAll 자원은 인증된 사용자에게도 허용된다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        webTestClient.get()
            .uri("/all-users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `유효한 access credential이면 보호 자원에 접근할 수 있다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val response = webTestClient.get()
            .uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()

        response.expectStatus().isOk
    }

    @Test
    fun `유효하지 않은 access credential이면 401과 공통 오류 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "invalid-access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willThrow(InvalidAccessCredentialException())

        // when
        val response = webTestClient.get()
            .uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertEquals(WebFluxErrorResponses.unauthorized().code, response.code)
        assertEquals(WebFluxErrorResponses.unauthorized().message, response.message)
    }

    @Test
    fun `인증된 사용자가 권한이 없는 자원에 접근하면 403을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willReturn(
            AuthenticateAccessCredentialUseCase.Result("user-001", "access-jti-001")
        )

        // when
        val response = webTestClient.get()
            .uri("/forbidden")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertEquals(WebFluxErrorResponses.forbidden().code, response.code)
        assertEquals(WebFluxErrorResponses.forbidden().message, response.message)
    }

    @Test
    fun `인증 중 데이터베이스 장애가 발생하면 503과 공통 오류 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willThrow(DataAccessResourceFailureException("Database is unavailable."))

        // when
        val response = webTestClient.get()
            .uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertEquals(WebFluxErrorResponses.serviceUnavailable().code, response.code)
        assertEquals(WebFluxErrorResponses.serviceUnavailable().message, response.message)
    }

    @Test
    fun `인증 중 예상하지 못한 예외가 발생하면 500과 공통 오류 응답을 반환한다`() = runTest {
        // given
        val serializedCredential = "access-token"
        given(authenticateAccessCredentialUseCase.authenticate(
            AuthenticateAccessCredentialUseCase.Command(serializedCredential)
        )).willThrow(IllegalStateException("Unexpected failure."))

        // when
        val response = webTestClient.get()
            .uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $serializedCredential")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody<ExceptionResponse>()
            .returnResult()
            .responseBody!!

        // then
        assertEquals(WebFluxErrorResponses.unexpectedError().code, response.code)
        assertEquals(WebFluxErrorResponses.unexpectedError().message, response.message)
    }

}

interface WebFluxSecurityTestApi {

    @PreAuthorize("isAnonymous()")
    suspend fun anonymousOnly(): ResponseEntity<Unit>

    @PreAuthorize("permitAll()")
    suspend fun allUsers(): ResponseEntity<Unit>

    @PreAuthorize("isAuthenticated()")
    suspend fun protected(): ResponseEntity<Unit>

    @PreAuthorize("hasAuthority('required')")
    suspend fun forbidden(): ResponseEntity<Unit>

}

@RestController
class WebFluxSecurityTestController : WebFluxSecurityTestApi {

    @GetMapping("/anonymous-only")
    override suspend fun anonymousOnly(): ResponseEntity<Unit> = ResponseEntity.ok().build()

    @GetMapping("/all-users")
    override suspend fun allUsers(): ResponseEntity<Unit> = ResponseEntity.ok().build()

    @GetMapping("/protected")
    override suspend fun protected(): ResponseEntity<Unit> = ResponseEntity.ok().build()

    @GetMapping("/forbidden")
    override suspend fun forbidden(): ResponseEntity<Unit> = ResponseEntity.ok().build()

}
