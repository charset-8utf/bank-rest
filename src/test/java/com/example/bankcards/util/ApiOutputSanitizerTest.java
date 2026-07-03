package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ApiOutputSanitizerTest {

    private ApiOutputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new ApiOutputSanitizer();
    }

    static Stream<Arguments> sanitizeCases() {
        return Stream.of(
                Arguments.of("/api/cards/1",                          "/api/cards/1"),
                Arguments.of("/api/<script>alert(1)</script>",        "/api/scriptalert(1)/script"),
                Arguments.of("/api/path&x=1",                        "/api/pathx=1"),
                Arguments.of("/path?a=\"value\"",                    "/path?a=value"),
                Arguments.of("/path?c='y'",                          "/path?c=y"),
                Arguments.of("/<>&\"'",                              "/"),
                Arguments.of("",                                     "")
        );
    }

    @ParameterizedTest(name = "sanitize({0}) = {1}")
    @MethodSource("sanitizeCases")
    void sanitizeUri_removesUnsafeChars(String input, String expected) {
        assertThat(sanitizer.sanitizeUri(input)).isEqualTo(expected);
    }
}
