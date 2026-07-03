package com.example.bankcards.security;

import com.example.bankcards.config.SecurityProperties;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.JwtProperties(
                        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                        86400000L,
                        604800000L
                ),
                new SecurityProperties.CardEncryptionProperties("U0VDUkVUQ0FSRE5VTUJFUktFWTEyMzQ1Njc4"),
                new SecurityProperties.RateLimitProperties(10, 10)
        );
        jwtService = new JwtService(properties);

        user = User.builder()
                .username("alice")
                .password("hashed")
                .roles(new HashSet<>(List.of(Role.builder().name(RoleType.USER).build())))
                .build();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_differentUser_returnsFalse() {
        String token = jwtService.generateToken(user);

        User other = User.builder()
                .username("bob")
                .password("hashed")
                .roles(new HashSet<>())
                .build();

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }
}
