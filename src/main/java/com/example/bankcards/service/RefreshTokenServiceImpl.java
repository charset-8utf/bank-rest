package com.example.bankcards.service;

import com.example.bankcards.config.SecurityProperties;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidRefreshTokenException;
import com.example.bankcards.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional
    public String create(User user) {
        long ttlMs = securityProperties.jwt().refreshExpiration();
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(ttlMs / 1000))
                .build();
        return refreshTokenRepository.save(token).getToken();
    }

    @Override
    @Transactional
    public User validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token не найден или уже использован"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidRefreshTokenException("Refresh token истёк");
        }

        return refreshToken.getUser();
    }

    @Override
    @Transactional
    public void delete(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}