package com.springboot.EduSystem.security.internal;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "t_internal_api_key")
public class InternalApiKey {
    @Id
    @Column(name = "key_id", nullable = false, length = 64, updatable = false)
    private String keyId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    //32-byte SHA-256
    @Column(name = "key_hash", nullable = false, length = 32)
    private byte[] hash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "t_internal_api_key_authority",
            joinColumns = @JoinColumn(name = "key_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(columnNames = {"key_id", "authority"}))
    @Column(name = "authority", nullable = false, length = 100)
    private Set<String> authorities = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public InternalApiKey() {
    }

    @PrePersist
    private void beforeInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public byte[] getHash() { return hash == null ? null : hash.clone(); }
    public void setHash(byte[] hash) {
        if (hash == null || hash.length != 32) {
            throw new IllegalArgumentException("API key hash must be a 32-byte SHA-256 digest");
        }
        this.hash = hash.clone();
    }

    public Set<String> getAuthorities() { return Set.copyOf(authorities); }
    public void setAuthorities(Set<String> authorities) {
        Set<String> copy = Set.copyOf(authorities);
        this.authorities.clear();
        this.authorities.addAll(copy);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
