package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserProfile;
import com.example.bankcards.exception.InvalidRefreshTokenException;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserProfileRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().name(RoleType.USER).build();
    }

    @Test
    void register_success_returnsTokenPair() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "pass123");

        when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getEmail()).isEqualTo("alice@test.com");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly(RoleType.USER);
    }

    @Test
    void register_usernameExists_throwsException() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "pass123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailExists_throwsException() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "pass123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice@test.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsTokenPair() {
        LoginRequest request = new LoginRequest("alice", "pass123");
        User user = User.builder().username("alice").roles(new HashSet<>(List.of(userRole))).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.create(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken
                        && "alice".equals(auth.getPrincipal()))
        );
    }

    @Test
    void refresh_validToken_returnsNewTokenPair() {
        User user = User.builder().username("alice").roles(new HashSet<>(List.of(userRole))).build();
        when(refreshTokenService.validate("old-refresh")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("new-access");
        when(refreshTokenService.create(user)).thenReturn("new-refresh");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).delete("old-refresh");
    }

    @Test
    void refresh_invalidToken_throwsException() {
        when(refreshTokenService.validate("bad-token"))
                .thenThrow(new InvalidRefreshTokenException("Refresh token не найден или уже использован"));

        RefreshTokenRequest request = new RefreshTokenRequest("bad-token");
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void logout_withBothTokens_blacklistsAndDeletes() {
        authService.logout("jti-123", "refresh-abc");

        verify(tokenBlacklistService).blacklist("jti-123");
        verify(refreshTokenService).delete("refresh-abc");
    }

    @Test
    void logout_withoutRefreshToken_onlyBlacklists() {
        authService.logout("jti-123", null);

        verify(tokenBlacklistService).blacklist("jti-123");
        verifyNoInteractions(refreshTokenService);
    }
}