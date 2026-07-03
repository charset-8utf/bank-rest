package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Имя пользователя обязательно")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}
