package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ApiOutputSanitizer {

    private static final Pattern UNSAFE_CHARS = Pattern.compile("[<>\"'&]");

    public String sanitizeUri(String uri) {
        return UNSAFE_CHARS.matcher(uri).replaceAll("");
    }
}
