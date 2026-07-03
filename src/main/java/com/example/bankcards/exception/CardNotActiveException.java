package com.example.bankcards.exception;

public class CardNotActiveException extends BankCardsException {

    public CardNotActiveException(String message) {
        super(message);
    }
}