package com.springboot.EduSystem.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {
    @Bean
    Clock jwtClock() {
        return Clock.systemUTC();
    }

    @Bean
    RSAPublicKey jwtPublicKey(@Value("${app.jwt.public-key}") Resource resource) throws IOException {
        try (InputStream input = resource.getInputStream()) {
            RSAPublicKey key = RsaKeyConverters.x509().convert(input);
            if (key == null || key.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("JWT public key must be an RSA key of at least 2048 bits");
            }
            return key;
        }
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey, Clock clock,
                          @Value("${app.jwt.issuer}") String issuer,
                          @Value("${app.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtAudienceValidator(audience),
                // Require login at expiry, without the default timestamp clock-skew grace period.
                new JwtClaimValidator<Instant>("exp", expiresAt ->
                        expiresAt != null && clock.instant().isBefore(expiresAt))
        ));
        return decoder;
    }
}
