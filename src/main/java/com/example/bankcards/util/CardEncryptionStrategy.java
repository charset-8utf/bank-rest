package com.example.bankcards.util;

public interface CardEncryptionStrategy {
    String encrypt(String cardNumber);
    String decrypt(String encryptedCardNumber);
}
