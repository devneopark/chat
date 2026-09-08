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

/**
 * Spring Security의 인증 요청을 access credential 인증 유즈케이스로 연결한다.
 *
 * 인증 성공 시 principal에는 userId를, credentials에는 후속 폐기에 사용할 JTI를 저장한다.
 * credential 오류는 401 흐름으로, 인증 인프라 장애는 5xx 흐름으로 변환한다.
 */
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
