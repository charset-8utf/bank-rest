package com.example.bankcards.exception;

public class InsufficientFundsException extends BankCardsException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}