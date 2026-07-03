package com.example.bankcards.util;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CardNumberValidatorTest {

    private final CardNumberValidator validator = new CardNumberValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111111",
            "5500005555555559",
            "4012888888881881",
            "0000000000000000"
    })
    void isValid_validLuhnNumbers_returnsTrue(String number) {
        assertThat(validator.isValid(number, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111112",
            "1234567890123456"
    })
    void isValid_failsLuhn_returnsFalse(String number) {
        assertThat(validator.isValid(number, context)).isFalse();
    }

    @Test
    void isValid_tooShort_returnsFalse() {
        assertThat(validator.isValid("411111111111111", context)).isFalse();
    }

    @Test
    void isValid_tooLong_returnsFalse() {
        assertThat(validator.isValid("41111111111111119", context)).isFalse();
    }

    @Test
    void isValid_containsLetters_returnsFalse() {
        assertThat(validator.isValid("411111111111111A", context)).isFalse();
    }

    @Test
    void isValid_null_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }
}
