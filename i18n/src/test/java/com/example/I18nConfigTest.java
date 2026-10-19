package com.example;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.i18n.I18NProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nConfigTest {

    private final Locale originalDefault = Locale.getDefault();

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(originalDefault);
    }

    @Test
    void serverDefaultLocaleDoesNotLeakIntoTranslations() {
        // English has no translations file of its own, so its texts must come
        // from the fallback file, not from the server's default locale.
        Locale.setDefault(SupportedLocales.GERMAN);
        I18NProvider provider = new I18nConfig().i18nProvider();

        assertEquals("Thank you for your order",
                provider.getTranslation("uc1.title", SupportedLocales.ENGLISH));
    }
}
