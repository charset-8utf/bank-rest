package com.example.bankcards.service;

import com.example.bankcards.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private RefreshTokenCleanupService cleanupService;

    @Test
    void deleteExpiredTokens_callsRepositoryWithCurrentTime() {
        when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
        LocalDateTime before = LocalDateTime.now();

        cleanupService.deleteExpiredTokens();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteExpiredBefore(captor.capture());
        assertThat(captor.getValue()).isAfterOrEqualTo(before);
    }

    @Test
    void deleteExpiredTokens_deletesExpired_logsCount() {
        when(refreshTokenRepository.deleteExpiredBefore(any())).thenReturn(5);

        cleanupService.deleteExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredBefore(any());
    }
}