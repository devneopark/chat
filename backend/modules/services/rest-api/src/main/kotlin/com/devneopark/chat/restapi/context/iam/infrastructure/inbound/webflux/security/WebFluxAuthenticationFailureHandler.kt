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

/**
 * 인증 필터에서 발생한 실패를 공통 HTTP 응답으로 변환한다.
 *
 * 잘못된 credential은 401로, 데이터베이스·트랜잭션 장애는 503으로, 그 외 예기치 않은 장애는 500으로 응답한다.
 */
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
