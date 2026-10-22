package com.springboot.EduSystem.security.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyAuthenticator {
    private final InternalApiKeyRepository repository;
    private final Clock clock;

    public ApiKeyAuthenticator(InternalApiKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Authentication authenticate(String rawKey) {
        ParsedApiKey parsed = ParsedApiKey.parse(rawKey);
        InternalApiKey key = repository.findById(parsed.keyId())
                .orElseThrow(this::invalidKey);

        Instant now = clock.instant();
        if (!key.isEnabled() || key.isRevoked()
                || (key.getExpiresAt() != null && !now.isBefore(key.getExpiresAt()))) {
            throw invalidKey();
        }

        byte[] storedHash = key.getHash();
        if (storedHash == null || !MessageDigest.isEqual(storedHash, sha256(rawKey))) {
            throw invalidKey();
        }

        var authorities = key.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new).toList();
        return UsernamePasswordAuthenticationToken.authenticated(
                key.getClientId(), null, authorities);
    }

    private static byte[] sha256(String rawKey) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BadCredentialsException invalidKey() {
        return new BadCredentialsException("Invalid service credential");
    }
}
