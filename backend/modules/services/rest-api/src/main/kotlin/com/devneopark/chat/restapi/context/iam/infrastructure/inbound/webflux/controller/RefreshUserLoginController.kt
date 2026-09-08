package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.iam.application.port.inbound.RenewalAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification.RefreshUserLoginApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import kotlin.time.toJavaDuration
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@RestController
class RefreshUserLoginController(

    private val renewalAuthenticationUseCase: RenewalAuthenticationUseCase,

    private val clock: Clock,

    private val cookieProperties: CookieProperties

) : RefreshUserLoginApi {

    override suspend fun refresh(refreshToken: String): ResponseEntity<RefreshUserLoginApi.Response> {
        val command = RenewalAuthenticationUseCase.Command(refreshToken)
        val result = renewalAuthenticationUseCase.renewal(command)
        val accessCredential = result.accessCredential
        val response = RefreshUserLoginApi.Response(
            accessCredential.serializedValue,
            accessCredential.expiresAt.toJavaInstant()
        )
        val renewalCredential = result.renewalCredential
        val refreshTokenExpiresAt = renewalCredential.expiresAt
        val now = clock.instant().toKotlinInstant()
        val maxAge = refreshTokenExpiresAt.minus(now)
        val cookie = ResponseCookie.from(cookieProperties.name)
            .value(renewalCredential.serializedValue)
            .httpOnly(cookieProperties.httpOnly)
            .secure(cookieProperties.secure)
            .sameSite(cookieProperties.sameSite)
            .maxAge(maxAge.toJavaDuration())
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(response)
    }

    @Component
    data class CookieProperties(

        @Value($$"${chat.infrastructure.auth.cookie.name}")
        val name: String,

        @Value($$"${chat.infrastructure.auth.cookie.httpOnly}")
        val httpOnly: Boolean,

        @Value($$"${chat.infrastructure.auth.cookie.secure}")
        val secure: Boolean,

        @Value($$"${chat.infrastructure.auth.cookie.sameSite}")
        val sameSite: String

    )

}
