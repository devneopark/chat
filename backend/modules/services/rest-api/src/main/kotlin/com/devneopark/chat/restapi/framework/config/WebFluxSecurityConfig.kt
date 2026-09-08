package com.devneopark.chat.restapi.framework.config

import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security.WebFluxAccessCredentialAuthenticationManager
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security.WebFluxAuthenticationEntryPoint
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security.WebFluxAuthenticationFailureHandler
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security.WebFluxBearerTokenAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache
import tools.jackson.databind.json.JsonMapper

/**
 * WebFlux Security 조립 설정이다.
 *
 * 교환 레벨은 허용하고 실제 인증·인가 정책은 API 인터페이스의 method security와
 * 사용자 정의 access credential 필터가 담당한다.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(proxyTargetClass = true)
class WebFluxSecurityConfig(

    private val jsonMapper: JsonMapper,

    private val authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase

) {

    @Bean
    fun webFluxSecurityWebFilterChain(
        http: ServerHttpSecurity,
    ): SecurityWebFilterChain {
        val authenticationEntryPoint = WebFluxAuthenticationEntryPoint(jsonMapper)

        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .requestCache {
                it.requestCache(NoOpServerRequestCache.getInstance())
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
            }
            .authorizeExchange {
                it.anyExchange().permitAll()
            }
            .addFilterAt(
                buildAuthenticationWebFilter(authenticationEntryPoint),
                SecurityWebFiltersOrder.AUTHENTICATION
            )
            .build()
    }

    private fun buildAuthenticationWebFilter(
        authenticationEntryPoint: WebFluxAuthenticationEntryPoint,
    ): AuthenticationWebFilter {
        val manager = WebFluxAccessCredentialAuthenticationManager(authenticateAccessCredentialUseCase)
        val filter = AuthenticationWebFilter(manager)
        val converter = WebFluxBearerTokenAuthenticationConverter()
        filter.setServerAuthenticationConverter(converter)
        filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        val failureHandler = WebFluxAuthenticationFailureHandler(authenticationEntryPoint, jsonMapper)
        filter.setAuthenticationFailureHandler(failureHandler)
        return filter
    }

}
