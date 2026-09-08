package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.TraceIdAssigningFilter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.test.assertTrue

@ActiveProfiles("test")
@WebFluxTest(controllers = [ TraceIdAssigningTest.TestController::class ])
@Import(TraceIdAssigningFilter::class, TraceIdAssigningTest.TestController::class)
class TraceIdAssigningTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `모든 응답의 헤더에는 항상 x-request-id 값이 포함된다`() {
        webTestClient.get()
            .uri("/trace-ids")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .value("x-request-id") {
                assertTrue(it.isNotBlank())
            }

        webTestClient.get()
            .uri("/${UUID.randomUUID()}")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .value("x-request-id") {
                assertTrue(it.isNotBlank())
            }
    }

    @RestController
    class TestController {

        @GetMapping("/trace-ids")
        suspend fun getString(): ResponseEntity<String> {
            return ResponseEntity.ok("OK")
        }

    }

}