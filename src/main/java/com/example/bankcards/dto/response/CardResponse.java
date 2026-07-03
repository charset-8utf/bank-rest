package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardResponse(
        Long id,
        String maskedCardNumber,
        String ownerUsername,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
