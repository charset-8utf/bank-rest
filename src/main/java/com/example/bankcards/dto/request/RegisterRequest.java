package com.example.bankcards.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 3, max = 64, message = "Имя пользователя должно содержать от 3 до 64 символов")
        String username,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, max = 128, message = "Пароль должен содержать от 6 до 128 символов")
        String password
) {}
