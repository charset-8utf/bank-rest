package com.example.bankcards.event;

import com.example.bankcards.entity.Card;

import java.math.BigDecimal;

public record TransferCompletedEvent(Card from, Card to, BigDecimal amount) {}
