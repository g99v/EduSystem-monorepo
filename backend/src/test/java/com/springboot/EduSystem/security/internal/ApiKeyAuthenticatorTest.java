package com.springboot.EduSystem.security.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticatorTest {
    private static final String RAW_KEY = "kong-test." + "A".repeat(43);
    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
    private InternalApiKeyRepository repository;
    private ApiKeyAuthenticator authenticator;
    private InternalApiKey key;

    @BeforeEach
    void setup() throws Exception {
        repository = mock(InternalApiKeyRepository.class);
        authenticator = new ApiKeyAuthenticator(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        key = new InternalApiKey();
        key.setKeyId("kong-test");
        key.setClientId("kong");
        key.setHash(MessageDigest.getInstance("SHA-256").digest(RAW_KEY.getBytes(StandardCharsets.UTF_8)));
        key.setAuthorities(Set.of("INTERNAL_AUTH_VERIFY"));
    }

    @Test
    void validKeyAuthenticatesStoredClientAndAuthoritiesWithoutRetainingSecret() {
        when(repository.findById("kong-test")).thenReturn(Optional.of(key));
        var result = authenticator.authenticate(RAW_KEY);
        assertTrue(result.isAuthenticated());
        assertEquals("kong", result.getName());
        assertNull(result.getCredentials());
        assertEquals(Set.of("INTERNAL_AUTH_VERIFY"), result.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"missing-separator", "id.short", ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "id.secret.with.dots"})
    void malformedKeyNeverReachesRepository(String rawKey) {
        assertThrows(BadCredentialsException.class, () -> authenticator.authenticate(rawKey));
        verifyNoInteractions(repository);
    }

    @Test
    void oversizedKeyNeverReachesRepository() {
        assertThrows(BadCredentialsException.class, () -> authenticator.authenticate("A".repeat(109)));
        verifyNoInteractions(repository);
    }

    @Test
    void unknownKeyIsRejected() {
        when(repository.findById("kong-test")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> authenticator.authenticate(RAW_KEY));
    }

    @Test
    void correctIdWithWrongSecretIsRejected() {
        when(repository.findById("kong-test")).thenReturn(Optional.of(key));
        assertThrows(BadCredentialsException.class,
                () -> authenticator.authenticate("kong-test." + "B".repeat(43)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "revoked", "expired", "expires-now"})
    void unusableStoredKeysAreRejected(String state) {
        when(repository.findById("kong-test")).thenReturn(Optional.of(key));
        switch (state) {
            case "disabled" -> key.setEnabled(false);
            case "revoked" -> key.setRevoked(true);
            case "expired" -> key.setExpiresAt(NOW.minusSeconds(1));
            case "expires-now" -> key.setExpiresAt(NOW);
        }
        assertThrows(BadCredentialsException.class, () -> authenticator.authenticate(RAW_KEY));
    }

    @Test
    void futureExpiryIsAcceptedAndRevocationIsObservedOnNextRequest() {
        when(repository.findById("kong-test")).thenReturn(Optional.of(key));
        key.setExpiresAt(NOW.plusSeconds(1));
        assertTrue(authenticator.authenticate(RAW_KEY).isAuthenticated());
        key.setRevoked(true);
        assertThrows(BadCredentialsException.class, () -> authenticator.authenticate(RAW_KEY));
        verify(repository, times(2)).findById("kong-test");
    }
}
