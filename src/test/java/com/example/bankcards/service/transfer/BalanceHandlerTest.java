package com.example.bankcards.service.transfer;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceHandlerTest {

    private BalanceHandler handler;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        handler = new BalanceHandler();
        User owner = User.builder().id(1L).build();
        fromCard = Card.builder().id(10L).owner(owner).status(CardStatus.ACTIVE).balance(BigDecimal.valueOf(500)).build();
        toCard   = Card.builder().id(20L).owner(owner).status(CardStatus.ACTIVE).balance(BigDecimal.ZERO).build();
    }

    @Test
    void handle_sufficientFunds_doesNotThrow() {
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(500), 1L);
        assertThatCode(() -> handler.handle(ctx)).doesNotThrowAnyException();
    }

    @Test
    void handle_insufficientFunds_throwsException() {
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(501), 1L);
        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(InsufficientFundsException.class);
    }
}