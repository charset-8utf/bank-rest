package com.example.bankcards.exception;

public class ResourceNotFoundException extends BankCardsException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}