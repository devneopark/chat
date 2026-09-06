package com.devneopark.chat.restapi.context.iam.infrastructure.inbound.webflux.security

import com.devneopark.chat.restapi.context.iam.application.exception.InvalidAccessCredentialException
import com.devneopark.chat.restapi.context.iam.application.port.inbound.AuthenticateAccessCredentialUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import reactor.core.publisher.Mono

class WebFluxAccessCredentialAuthenticationManager(

    private val authenticateAccessCredentialUseCase: AuthenticateAccessCredentialUseCase,

) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = mono {
        try {
            val serializedCredential = authentication.credentials as? String
                ?: throw BadCredentialsException("Invalid access credential.")
            val result = authenticateAccessCredentialUseCase.authenticate(
                AuthenticateAccessCredentialUseCase.Command(serializedCredential)
            )
            UsernamePasswordAuthenticationToken(
                result.userId,
                result.jti,
                emptyList()
            )
        } catch (exception: InvalidAccessCredentialException) {
            throw BadCredentialsException("Invalid access credential.", exception)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthenticationException) {
            throw exception
        } catch (exception: Exception) {
            throw AuthenticationServiceException("Authentication service failed.", exception)
        }
    }

}
