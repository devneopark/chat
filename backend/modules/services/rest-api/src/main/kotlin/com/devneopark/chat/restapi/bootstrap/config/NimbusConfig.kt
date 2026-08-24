package com.devneopark.chat.restapi.bootstrap.config

import com.devneopark.chat.restapi.context.iam.infrastructure.outbound.auth.NimbusAuthenticationCredentialManager.JwtProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

@Configuration
class NimbusConfig {

    @Bean
    fun jwtEncoder(jwtProperties: JwtProperties): JwtEncoder {
        val encodedSecretKey = jwtProperties.encodedSecretKey
        val keyBytes = Base64.getDecoder()
            .decode(encodedSecretKey)
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA512")
        return NimbusJwtEncoder.withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS512)
            .build()
    }

}