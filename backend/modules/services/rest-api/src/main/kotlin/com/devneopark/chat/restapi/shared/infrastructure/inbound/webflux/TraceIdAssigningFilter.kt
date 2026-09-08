package com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux

import com.devneopark.chat.libs.shared.application.identifier.HyphenlessUuidGenerator
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

private val idGen = HyphenlessUuidGenerator()

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdAssigningFilter : CoWebFilter() {

    override suspend fun filter(
        exchange: ServerWebExchange,
        chain: CoWebFilterChain
    ) {
        val traceId = idGen.generate()
        TraceIdContext.with(traceId) {
            exchange.response.headers.set("X-Request-ID", traceId)
            chain.filter(exchange)
        }
    }

}