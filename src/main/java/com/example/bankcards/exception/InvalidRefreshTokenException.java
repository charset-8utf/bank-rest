package com.example.bankcards.exception;

public class InvalidRefreshTokenException extends BankCardsException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}