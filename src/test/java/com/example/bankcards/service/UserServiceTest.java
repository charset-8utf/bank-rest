package com.example.bankcards.service;

import com.example.bankcards.dto.request.UserProfileRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.UserProfileResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserProfile;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserProfileRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User alice;
    private UserProfile aliceProfile;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().name(RoleType.USER).build();

        alice = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .roles(new HashSet<>(List.of(userRole)))
                .build();

        aliceProfile = UserProfile.builder()
                .id(10L)
                .user(alice)
                .firstName("Alice")
                .lastName("Smith")
                .phone("+71234567890")
                .build();
    }

    @Test
    void getUserById_found_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(1L, "alice", "alice@test.com", Set.of("USER"), LocalDateTime.now()));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllUsers_returnsPaginatedResponse() {
        Role userRole = Role.builder().name(RoleType.USER).build();
        List<User> users = List.of(
                alice,
                User.builder().id(2L).username("bob").email("bob@test.com").roles(new HashSet<>(List.of(userRole))).build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(users, pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(1L, "alice", "alice@test.com", Set.of("USER"), LocalDateTime.now()));

        PageResponse<UserResponse> response = userService.getAllUsers(pageable);

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).hasSize(2);
    }

    @Test
    void deleteUser_notFound_throwsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_found_revokesSessionsThenDeletesUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        var inOrder = inOrder(refreshTokenRepository, userProfileRepository, userRepository);
        inOrder.verify(refreshTokenRepository).deleteByUserId(1L);
        inOrder.verify(userProfileRepository).deleteByUserId(1L);
        inOrder.verify(userRepository).deleteById(1L);
    }

    @Test
    void getMyProfile_found_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(aliceProfile));

        UserProfileResponse response = userService.getMyProfile(1L);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.phone()).isEqualTo("+71234567890");
    }

    @Test
    void getMyProfile_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateMyProfile_success_updatesAndReturns() {
        UserProfileRequest request = new UserProfileRequest("Bob", "Jones", "+79998887766");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(aliceProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = userService.updateMyProfile(1L, request);

        assertThat(response.firstName()).isEqualTo("Bob");
        assertThat(response.lastName()).isEqualTo("Jones");
        assertThat(response.phone()).isEqualTo("+79998887766");
        verify(userProfileRepository).save(aliceProfile);
    }

    @Test
    void updateMyProfile_profileNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        UserProfileRequest request = new UserProfileRequest("A", "B", null);
        assertThatThrownBy(() -> userService.updateMyProfile(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userProfileRepository, never()).save(any());
    }
}
