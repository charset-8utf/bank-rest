package com.example.bankcards.util;

import com.example.bankcards.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptionStrategyTest {

    private AesGcmEncryptionStrategy strategy;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.JwtProperties("unused", 0L, 0L),
                new SecurityProperties.CardEncryptionProperties("U0VDUkVUQ0FSRE5VTUJFUktFWTEyMzQ1Njc4"),
                new SecurityProperties.RateLimitProperties(10, 10)
        );
        strategy = new AesGcmEncryptionStrategy(properties);
    }

    @Test
    void encryptAndDecrypt_roundtrip_returnsOriginal() {
        String cardNumber = "4111111111111111";
        String encrypted = strategy.encrypt(cardNumber);
        assertThat(strategy.decrypt(encrypted)).isEqualTo(cardNumber);
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertexts() {
        String cardNumber = "4111111111111111";
        assertThat(strategy.encrypt(cardNumber)).isNotEqualTo(strategy.encrypt(cardNumber));
    }

    @Test
    void decrypt_invalidBase64_throwsIllegalStateException() {
        assertThatThrownBy(() -> strategy.decrypt("not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ошибка дешифрования");
    }

    @Test
    void decrypt_validBase64ButCorruptedCiphertext_throwsIllegalStateException() {
        String corrupted = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> strategy.decrypt(corrupted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ошибка дешифрования");
    }
}