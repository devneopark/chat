package com.devneopark.chat.restapi.context.iam.infrastructure.outbound.crypto

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class BCryptUserPasswordVerifierTest {

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    lateinit var passwordVerifier: BCryptUserPasswordVerifier

    @Test
    fun `raw password와 해시된 password의 일치 여부를 password encoder로 확인한다`() = runTest {
        // given
        val rawPassword = "raw-password"
        val encodedPassword = "encoded-password"
        given(passwordEncoder.matches(rawPassword, encodedPassword))
            .willReturn(true)

        // when
        val result = passwordVerifier.matches(rawPassword, encodedPassword)

        // then
        assertTrue(result)
        verify(passwordEncoder).matches(rawPassword, encodedPassword)
    }

}
