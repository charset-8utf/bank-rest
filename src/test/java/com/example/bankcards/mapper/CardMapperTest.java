package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CardMapperTest {

    private CardMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CardMapperImpl();
    }

    @Test
    void toResponse_bothNull_returnsNull() {
        assertThat(mapper.toResponse(null, null)).isNull();
    }

    @Test
    void toResponse_withOwner_mapsAllFields() {
        User owner = User.builder().username("alice").build();
        Card card = Card.builder()
                .id(1L)
                .owner(owner)
                .expiryDate(LocalDate.of(2028, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        CardResponse response = mapper.toResponse(card, "**** **** **** 1234");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.maskedCardNumber()).isEqualTo("**** **** **** 1234");
        assertThat(response.ownerUsername()).isEqualTo("alice");
        assertThat(response.expiryDate()).isEqualTo(LocalDate.of(2028, 12, 31));
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.balance()).isEqualByComparingTo("500");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
    }

    @Test
    void toResponse_nullCard_nonNullMasked_mapsOnlyMasked() {
        CardResponse response = mapper.toResponse(null, "**** **** **** 9999");

        assertThat(response).isNotNull();
        assertThat(response.maskedCardNumber()).isEqualTo("**** **** **** 9999");
        assertThat(response.id()).isNull();
        assertThat(response.ownerUsername()).isNull();
        assertThat(response.status()).isNull();
    }

    @Test
    void toResponse_nullOwner_ownerUsernameIsNull() {
        Card card = Card.builder()
                .id(2L)
                .owner(null)
                .expiryDate(LocalDate.of(2028, 12, 31))
                .status(CardStatus.BLOCKED)
                .build();

        CardResponse response = mapper.toResponse(card, "**** **** **** 5678");

        assertThat(response.ownerUsername()).isNull();
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
    }
}
