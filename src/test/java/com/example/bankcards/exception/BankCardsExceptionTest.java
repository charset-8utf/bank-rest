package com.example.bankcards.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankCardsExceptionTest {

    @Test
    void constructor_withMessage_storesMessage() {
        BankCardsException ex = new BankCardsException("test error");
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_storesBoth() {
        Throwable cause = new IllegalStateException("root cause");
        BankCardsException ex = new BankCardsException("test error", cause);
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
