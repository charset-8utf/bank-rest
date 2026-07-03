package com.example.bankcards.dto.response;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone
) {}
