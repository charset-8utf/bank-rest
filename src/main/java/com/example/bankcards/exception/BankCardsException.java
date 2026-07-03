package com.example.bankcards.exception;

public class BankCardsException extends RuntimeException {

    public BankCardsException(String message) {
        super(message);
    }

    public BankCardsException(String message, Throwable cause) {
        super(message, cause);
    }
}
