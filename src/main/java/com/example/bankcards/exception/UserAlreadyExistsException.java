package com.example.bankcards.exception;

public class UserAlreadyExistsException extends BankCardsException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}