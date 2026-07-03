package com.example.bankcards.service;

import com.example.bankcards.dto.request.UserProfileRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.UserProfileResponse;
import com.example.bankcards.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(Long id);

    void deleteUser(Long id);

    UserProfileResponse getMyProfile(Long userId);

    UserProfileResponse updateMyProfile(Long userId, UserProfileRequest request);
}
