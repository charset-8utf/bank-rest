package com.example.bankcards.controller;

import com.example.bankcards.dto.request.UserProfileRequest;
import com.example.bankcards.dto.response.UserProfileResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Профиль", description = "Управление профилем текущего пользователя")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Получить свой профиль")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal User user) {
        return userService.getMyProfile(user.getId());
    }

    @PutMapping("/profile")
    @Operation(summary = "Обновить свой профиль")
    public UserProfileResponse updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileRequest request) {
        return userService.updateMyProfile(user.getId(), request);
    }
}
