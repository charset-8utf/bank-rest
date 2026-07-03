package com.example.bankcards.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @Size(max = 64) String firstName,
        @Size(max = 64) String lastName,
        @Pattern(regexp = "^\\+?[0-9 \\-()]{7,20}$", message = "Некорректный формат телефона")
        String phone
) {}
