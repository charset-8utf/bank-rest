package com.example.bankcards.service;

import com.example.bankcards.config.CacheConfig;
import com.example.bankcards.dto.request.UserProfileRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.UserProfileResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserProfile;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserProfileRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final String USER_NOT_FOUND = "Пользователь не найден: ";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return new PageResponse<>(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true)
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        refreshTokenRepository.deleteByUserId(id);
        userProfileRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        log.info("Пользователь удалён: userId={}", id);
    }

    @Override
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль не найден для пользователя: " + userId));
        return toProfileResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль не найден для пользователя: " + userId));

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());

        userProfileRepository.save(profile);
        log.info("Профиль обновлён: userId={}", userId);
        return toProfileResponse(user, profile);
    }

    private UserProfileResponse toProfileResponse(User user, UserProfile profile) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone()
        );
    }
}
