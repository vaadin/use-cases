package com.example;

import jakarta.servlet.http.Cookie;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguagePreferenceTest {

    @Test
    void readsASupportedLanguageFromTheCookie() {
        Cookie[] cookies = { new Cookie("other", "x"),
                new Cookie(LanguagePreference.COOKIE, "he-IL") };

        assertEquals(Optional.of(SupportedLocales.HEBREW),
                LanguagePreference.fromCookies(cookies));
    }

    @Test
    void ignoresMissingAndUnsupportedLanguages() {
        assertEquals(Optional.empty(), LanguagePreference.fromCookies(null));
        assertEquals(Optional.empty(), LanguagePreference.fromCookies(
                new Cookie[] { new Cookie(LanguagePreference.COOKIE, "ja") }));
        assertEquals(Optional.empty(),
                LanguagePreference.fromCookies(new Cookie[] {
                        new Cookie(LanguagePreference.COOKIE, "garbage!") }));
    }
}
