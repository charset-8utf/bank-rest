package com.example.bankcards.service.transfer;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class OwnershipHandlerTest {

    private OwnershipHandler handler;
    private User owner;
    private User other;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        handler = new OwnershipHandler();
        owner = User.builder().id(1L).build();
        other = User.builder().id(2L).build();
        fromCard = Card.builder().id(10L).owner(owner).status(CardStatus.ACTIVE).balance(BigDecimal.valueOf(500)).build();
        toCard   = Card.builder().id(20L).owner(owner).status(CardStatus.ACTIVE).balance(BigDecimal.ZERO).build();
    }

    @Test
    void handle_bothCardsOwned_doesNotThrow() {
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatCode(() -> handler.handle(ctx)).doesNotThrowAnyException();
    }

    @Test
    void handle_fromCardNotOwned_throwsAccessDenied() {
        fromCard.setOwner(other);
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatThrownBy(() -> handler.handle(ctx)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void handle_toCardNotOwned_throwsAccessDenied() {
        toCard.setOwner(other);
        TransferContext ctx = new TransferContext(fromCard, toCard, BigDecimal.valueOf(100), 1L);
        assertThatThrownBy(() -> handler.handle(ctx)).isInstanceOf(AccessDeniedException.class);
    }
}