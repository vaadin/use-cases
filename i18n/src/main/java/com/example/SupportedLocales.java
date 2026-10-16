package com.example;

import java.util.List;
import java.util.Locale;

/**
 * The languages this application is translated to, in the order the language
 * picker lists them. The first one is the default: it is used when none of the
 * browser's preferred languages is supported, and its texts live in the
 * fallback file {@code vaadin-i18n/translations.properties}.
 */
public final class SupportedLocales {

    public static final Locale ENGLISH = Locale.of("en", "US");
    public static final Locale GERMAN = Locale.of("de", "DE");
    public static final Locale FINNISH = Locale.of("fi", "FI");
    public static final Locale ARABIC = Locale.of("ar", "EG");
    public static final Locale HEBREW = Locale.of("he", "IL");

    public static final List<Locale> ALL = List.of(ENGLISH, GERMAN, FINNISH,
            ARABIC, HEBREW);

    private SupportedLocales() {
    }

    /**
     * The name of the language in that language ("Deutsch", "suomi",
     * "العربية"), which is how a user who cannot read the current language
     * still finds their own in a picker.
     */
    public static String nativeName(Locale locale) {
        String name = locale.getDisplayLanguage(locale);
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }
}
