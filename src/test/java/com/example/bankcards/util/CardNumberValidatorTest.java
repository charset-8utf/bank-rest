package com.example.bankcards.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberValidatorTest {

    private final CardNumberValidator validator = new CardNumberValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111111",
            "5500005555555559",
            "4012888888881881",
            "0000000000000000"
    })
    void isValid_validLuhnNumbers_returnsTrue(String number) {
        assertThat(validator.isValid(number, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111112",
            "1234567890123456"
    })
    void isValid_failsLuhn_returnsFalse(String number) {
        assertThat(validator.isValid(number, null)).isFalse();
    }

    @Test
    void isValid_tooShort_returnsFalse() {
        assertThat(validator.isValid("411111111111111", null)).isFalse();
    }

    @Test
    void isValid_tooLong_returnsFalse() {
        assertThat(validator.isValid("41111111111111119", null)).isFalse();
    }

    @Test
    void isValid_containsLetters_returnsFalse() {
        assertThat(validator.isValid("411111111111111A", null)).isFalse();
    }

    @Test
    void isValid_null_returnsTrue() {
        // По спецификации Bean Validation, isValid должен возвращать true для null —
        // null обрабатывается аннотацией @NotBlank на уровне поля
        assertThat(validator.isValid(null, null)).isTrue();
    }
}