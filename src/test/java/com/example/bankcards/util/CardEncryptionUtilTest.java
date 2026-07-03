package com.example.bankcards.util;

import com.example.bankcards.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardEncryptionUtilTest {

    private CardEncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.JwtProperties("unused", 0L, 0L),
                new SecurityProperties.CardEncryptionProperties("U0VDUkVUQ0FSRE5VTUJFUktFWTEyMzQ1Njc4"),
                new SecurityProperties.RateLimitProperties(10, 10)
        );
        encryptionUtil = new CardEncryptionUtil(properties);
    }

    @Test
    void encryptAndDecrypt_roundtrip_returnsOriginal() {
        String cardNumber = "4111111111111111";
        String encrypted = encryptionUtil.encrypt(cardNumber);
        assertThat(encryptionUtil.decrypt(encrypted)).isEqualTo(cardNumber);
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertexts() {
        String cardNumber = "4111111111111111";
        String first = encryptionUtil.encrypt(cardNumber);
        String second = encryptionUtil.encrypt(cardNumber);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void decrypt_invalidBase64_throwsIllegalStateException() {
        assertThatThrownBy(() -> encryptionUtil.decrypt("not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ошибка дешифрования");
    }

    @Test
    void decrypt_validBase64ButCorruptedCiphertext_throwsIllegalStateException() {
        String corruptedCiphertext = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> encryptionUtil.decrypt(corruptedCiphertext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ошибка дешифрования");
    }
}
