package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security

import com.devneopark.chat.restapi.shared.infrastructure.inbound.webflux.WebFluxErrorResponses
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.transaction.TransactionException
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

class WebFluxAuthenticationFailureHandler(

    private val authenticationEntryPoint: WebFluxAuthenticationEntryPoint,

    private val jsonMapper: JsonMapper,

) : ServerAuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        exchange: WebFilterExchange,
        exception: AuthenticationException,
    ): Mono<Void> {
        if (exception !is AuthenticationServiceException) {
            return authenticationEntryPoint.commence(exchange.exchange, exception)
        }

        val (httpStatus, errorResponse) = when (exception.cause) {
            is DataAccessException, is TransactionException ->
                HttpStatus.SERVICE_UNAVAILABLE to WebFluxErrorResponses.serviceUnavailable()
            else ->
                HttpStatus.INTERNAL_SERVER_ERROR to WebFluxErrorResponses.unexpectedError()
        }

        val response = exchange.exchange.response
        response.statusCode = httpStatus
        response.headers.contentType = MediaType.APPLICATION_JSON
        val responseBodyBytes = jsonMapper.writeValueAsBytes(errorResponse)
        val bufferFactory = response.bufferFactory()
        val dataBuffer = bufferFactory.wrap(responseBodyBytes)
        val bodyMono = Mono.just(dataBuffer)
        return response.writeWith(bodyMono)
    }

}
