package com.example.bankcards.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long fromCardId,
        String fromCardMasked,
        Long toCardId,
        String toCardMasked,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {}
