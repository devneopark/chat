package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux

import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security.WebFluxAuthenticationEntryPoint
import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.WebFluxErrorResponses
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.BadCredentialsException
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals

class WebFluxSecurityExceptionHandlerTest {

    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `인증 실패시 401과 공통 오류 응답을 반환한다`() {
        // given
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        val entryPoint = WebFluxAuthenticationEntryPoint(jsonMapper)

        // when
        entryPoint.commence(exchange, BadCredentialsException("invalid credential")).block()

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, exchange.response.headers.contentType)
        val response = jsonMapper.readTree(exchange.response.bodyAsString.block())
        assertEquals(WebFluxErrorResponses.unauthorized().code, response["code"].asString())
        assertEquals(WebFluxErrorResponses.unauthorized().message, response["message"].asString())
    }

}
