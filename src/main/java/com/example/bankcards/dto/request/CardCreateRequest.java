package com.example.bankcards.dto.request;

import com.example.bankcards.util.ValidCardNumber;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CardCreateRequest(

        @NotNull(message = "ID владельца обязателен")
        Long ownerId,

        @NotBlank(message = "Номер карты обязателен")
        @ValidCardNumber
        String cardNumber,

        @NotNull(message = "Дата истечения обязательна")
        @Future(message = "Дата истечения должна быть в будущем")
        LocalDate expiryDate
) {}
