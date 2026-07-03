package com.example.bankcards.service;

import com.example.bankcards.config.SecurityProperties;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidRefreshTokenException;
import com.example.bankcards.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SecurityProperties securityProperties;
    @Mock private SecurityProperties.JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").build();
    }

    @Test
    void create_savesTokenAndReturnsValue() {
        when(securityProperties.jwt()).thenReturn(jwtProperties);
        when(jwtProperties.refreshExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String token = refreshTokenService.create(user);

        assertThat(token).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void validate_validToken_returnsUser() {
        RefreshToken token = RefreshToken.builder()
                .token("uuid-123")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        when(refreshTokenRepository.findByToken("uuid-123")).thenReturn(Optional.of(token));

        User result = refreshTokenService.validate("uuid-123");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void validate_tokenNotFound_throwsException() {
        when(refreshTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("bad"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validate_expiredToken_deletesAndThrows() {
        RefreshToken token = RefreshToken.builder()
                .token("expired")
                .user(user)
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validate("expired"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("истёк");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void delete_callsRepository() {
        refreshTokenService.delete("uuid-123");

        verify(refreshTokenRepository).deleteByToken("uuid-123");
    }
}
