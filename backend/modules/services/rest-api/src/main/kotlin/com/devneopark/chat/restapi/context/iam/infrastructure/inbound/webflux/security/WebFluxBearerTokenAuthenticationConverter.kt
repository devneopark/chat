package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

private const val BEARER_PREFIX = "Bearer "

class WebFluxBearerTokenAuthenticationConverter : ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val headers = exchange.request.headers
        val authorization = headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: return Mono.empty()
        if (!authorization.startsWith(BEARER_PREFIX)) {
            return Mono.empty()
        }

        val serializedCredential = authorization.removePrefix(BEARER_PREFIX)
        if (serializedCredential.isBlank()) {
            return Mono.empty()
        }

        return Mono.just(
            UsernamePasswordAuthenticationToken.unauthenticated(
                serializedCredential,
                serializedCredential
            )
        )
    }

}
