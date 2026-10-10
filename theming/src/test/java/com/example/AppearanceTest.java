package com.example;

import jakarta.servlet.http.Cookie;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppearanceTest {

    @Test
    void readsRememberedChoicesFromCookies() {
        Cookie[] cookies = { new Cookie(Appearance.ACCENT, "%237c3aed"),
                new Cookie(Appearance.THEME, "LUMO"),
                new Cookie("other", "x") };

        Map<String, String> values = Appearance.cookies(cookies);

        assertEquals("#7c3aed", values.get(Appearance.ACCENT));
        assertEquals("LUMO", values.get(Appearance.THEME));
        assertEquals(Map.of(), Appearance.cookies(null));
    }
}
