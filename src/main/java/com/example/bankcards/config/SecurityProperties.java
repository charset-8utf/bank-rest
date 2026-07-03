package com.example.bankcards.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.security")
public record SecurityProperties(
        JwtProperties jwt,
        CardEncryptionProperties cardEncryption,
        RateLimitProperties rateLimit
) {
    public record JwtProperties(String secret, long expiration, long refreshExpiration) {}
    public record CardEncryptionProperties(String secret) {}
    public record RateLimitProperties(int capacity, int refillPerMinute) {}
}