package com.devneopark.chat.restapi.context.iam.application.service

import com.devneopark.chat.restapi.context.iam.application.port.inbound.RevokeAuthenticationUseCase
import com.devneopark.chat.restapi.context.iam.application.port.outbound.AuthenticationGrantRepositoryPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.verify

@ExtendWith(MockitoExtension::class)
class RevokeAuthenticationServiceTest {

    @Mock
    lateinit var authenticationGrantRepositoryPort: AuthenticationGrantRepositoryPort

    @InjectMocks
    lateinit var revokeAuthenticationService: RevokeAuthenticationService

    @Test
    fun `인증정보를 access jti로 삭제한다`() = runTest {
        // given
        val command = RevokeAuthenticationUseCase.Command("access-jti-001")

        // when
        revokeAuthenticationService.revoke(command)

        // then
        verify(authenticationGrantRepositoryPort).deleteByJti(command.jti)
    }

}
