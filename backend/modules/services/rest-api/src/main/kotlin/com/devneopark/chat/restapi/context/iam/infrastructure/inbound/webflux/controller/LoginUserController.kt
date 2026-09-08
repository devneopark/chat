package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.controller

import com.devneopark.chat.restapi.context.iam.application.port.inbound.GrantAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.specification.LoginUserApi
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
class LoginUserController(

    private val grantAuthenticationUseCase: GrantAuthenticationUseCase,

    private val clock: Clock,

    private val cookieProperties: CookieProperties

) : LoginUserApi {

    override suspend fun login(body: LoginUserApi.Request): ResponseEntity<LoginUserApi.Response> {
        val command = GrantAuthenticationUseCase.Command(
            body.principal!!,
            body.rawPassword!!
        )
        val result = grantAuthenticationUseCase.grant(command)
        val accessCredential = result.accessCredential
        val response = LoginUserApi.Response(
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
