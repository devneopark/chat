package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.WebFluxErrorResponses
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

/** 인증되지 않은 WebFlux 요청에 공통 401 오류 응답을 작성하는 진입점이다. */
class WebFluxAuthenticationEntryPoint(

    private val jsonMapper: JsonMapper,

) : ServerAuthenticationEntryPoint {

    override fun commence(
        exchange: ServerWebExchange,
        denied: AuthenticationException
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON
        val errorResponse = WebFluxErrorResponses.unauthorized()
        val responseBodyBytes = jsonMapper.writeValueAsBytes(errorResponse)
        val bufferFactory = response.bufferFactory()
        val dataBuffer = bufferFactory.wrap(responseBodyBytes)
        val bodyMono = Mono.just(dataBuffer)
        return response.writeWith(bodyMono)
    }

}
