package com.example.bankcards.service.transfer;

import com.example.bankcards.entity.Card;

import java.math.BigDecimal;

public record TransferContext(Card from, Card to, BigDecimal amount, Long userId) {}
