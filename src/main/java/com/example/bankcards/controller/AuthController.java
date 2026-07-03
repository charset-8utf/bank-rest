package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, вход, обновление токенов и выход")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Зарегистрировать нового пользователя")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Войти и получить пару токенов (access + refresh)")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновить access token по refresh token (rotation: старый refresh инвалидируется)")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Выйти: инвалидировать access token (JTI blacklist) и refresh token")
    @SecurityRequirement(name = "bearerAuth")
    @SuppressWarnings("java:S2589") // @RequestBody(required=false) passes null when no body is sent — SonarLint false positive
    public void logout(
            HttpServletRequest request,
            @RequestBody(required = false) RefreshTokenRequest body) {

        String authHeader = request.getHeader("Authorization");
        String jti = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                jti = jwtService.extractJti(authHeader.substring(7));
            } catch (Exception e) {
                log.debug("Не удалось извлечь JTI при выходе: {}", e.getMessage());
            }
        }
        authService.logout(jti, body != null ? body.refreshToken() : null);
    }
}