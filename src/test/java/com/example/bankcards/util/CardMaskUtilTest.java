package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CardMaskUtilTest {

    private CardMaskUtil maskUtil;

    @BeforeEach
    void setUp() {
        maskUtil = new CardMaskUtil();
    }

    static Stream<Arguments> maskCases() {
        return Stream.of(
                Arguments.of("4111111111111234",     "**** **** **** 1234"),
                Arguments.of("4111 1111 1111 1234",  "**** **** **** 1234"),
                Arguments.of("4111-1111-1111-5678",  "**** **** **** 5678"),
                Arguments.of("1234",                 "**** **** **** 1234"),
                Arguments.of("123",                  "**** **** **** ****"),
                Arguments.of("",                     "**** **** **** ****")
        );
    }

    @ParameterizedTest(name = "mask({0}) = {1}")
    @MethodSource("maskCases")
    void mask_variousInputs_producesExpectedResult(String input, String expected) {
        assertThat(maskUtil.mask(input)).isEqualTo(expected);
    }
}
