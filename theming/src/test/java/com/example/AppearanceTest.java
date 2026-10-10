package com.example;

import jakarta.servlet.http.Cookie;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.page.ColorScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppearanceTest {

    @Test
    void readsCookieValues() {
        Cookie[] cookies = { new Cookie(Appearance.ACCENT, "%237c3aed"),
                new Cookie(Appearance.THEME, "LUMO"),
                new Cookie("other", "x") };

        Map<String, String> values = Appearance.cookies(cookies);

        assertEquals("#7c3aed", values.get(Appearance.ACCENT));
        assertEquals("LUMO", values.get(Appearance.THEME));
        assertEquals(Map.of(), Appearance.cookies(null));
    }

    @Test
    void restoresRememberedChoices() {
        Appearance appearance = new Appearance(Map.of(Appearance.THEME, "LUMO",
                Appearance.COLOR_SCHEME, "DARK", Appearance.ACCENT, "#7c3aed",
                Appearance.FONT_SIZE, "LARGE", Appearance.HIGH_CONTRAST, "true",
                Appearance.REDUCE_MOTION, "true"));

        assertEquals(BaseTheme.LUMO, appearance.theme().peek());
        assertEquals(ColorScheme.Value.DARK, appearance.colorScheme().peek());
        assertEquals("#7c3aed", appearance.accent().peek());
        assertEquals(FontSize.LARGE, appearance.fontSize().peek());
        assertTrue(appearance.highContrast().peek());
        assertTrue(appearance.reduceMotion().peek());
    }

    @Test
    void invalidRememberedValuesFallBackToDefaults() {
        Appearance appearance = new Appearance(Map.of(Appearance.THEME,
                "BOOTSTRAP", Appearance.COLOR_SCHEME, "", Appearance.ACCENT,
                "red;}body{display:none", Appearance.FONT_SIZE, "huge",
                Appearance.HIGH_CONTRAST, "yes"));

        assertEquals(BaseTheme.AURA, appearance.theme().peek());
        assertEquals(ColorScheme.Value.SYSTEM, appearance.colorScheme().peek());
        // Never reaches the CSS custom properties set on the page.
        assertNull(appearance.accent().peek());
        assertEquals(FontSize.DEFAULT, appearance.fontSize().peek());
        assertFalse(appearance.highContrast().peek());
        assertFalse(appearance.reduceMotion().peek());

        // What a reset accent writes back.
        assertNull(
                new Appearance(Map.of(Appearance.ACCENT, "")).accent().peek());
    }
}
