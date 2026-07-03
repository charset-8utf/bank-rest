package com.example.bankcards.service.transfer;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotActiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardStatusHandlerTest {

    private CardStatusHandler handler;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        handler = new CardStatusHandler();
        User owner = User.builder().id(1L).build();
        fromCard = Card.builder().id(10L).owner(owner).status(CardStatus.ACTIVE).balance(BigDecimal.valueOf(500)).build();
        toCard   = Card.builder().id(20L).owner(owner).status(CardStatus.ACTIVE).balance(BigDecimal.ZERO).build();
    }

    @Test
    void handle_bothCardsActive_doesNotThrow() {
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatCode(() -> handler.handle(ctx)).doesNotThrowAnyException();
    }

    @Test
    void handle_fromCardBlocked_throwsWithSourceRole() {
        fromCard.setStatus(CardStatus.BLOCKED);
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("источник");
    }

    @Test
    void handle_toCardBlocked_throwsWithDestinationRole() {
        toCard.setStatus(CardStatus.BLOCKED);
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("получатель");
    }

    @Test
    void handle_fromCardExpired_throwsWithSourceRole() {
        fromCard.setStatus(CardStatus.EXPIRED);
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("источник");
    }
}