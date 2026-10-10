package com.example.uc10;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.example.SupportedLocales;

/**
 * A text entered by users in several languages, such as the name of a product
 * category: one value per language, keyed by language code ({@code "de"}).
 * <p>
 * It is a plain value of the entity it belongs to, so it is stored, copied and
 * shared together with the rest of the entity. Looking up a text falls back
 * from the exact locale to its language and then to the default language.
 */
public record LocalizedText(Map<String, String> values) {

    static final Locale DEFAULT = SupportedLocales.ENGLISH;

    public LocalizedText {
        values = Map.copyOf(values);
    }

    /** A text and the language it is actually in. */
    public record Resolved(String text, String language, boolean fallback) {
    }

    public static LocalizedText of(String... languageAndText) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < languageAndText.length; i += 2) {
            values.put(languageAndText[i], languageAndText[i + 1]);
        }
        return new LocalizedText(values);
    }

    /**
     * The text for {@code locale}: its exact tag ({@code de-CH}), then its
     * language ({@code de}), then the default language.
     */
    public Resolved in(Locale locale) {
        List<String> candidates = Stream.of(locale.toLanguageTag(),
                locale.getLanguage(), DEFAULT.getLanguage()).distinct()
                .toList();
        for (String candidate : candidates) {
            String text = values.get(candidate);
            if (text != null && !text.isBlank()) {
                return new Resolved(text, candidate,
                        !candidate.equals(locale.toLanguageTag())
                                && !candidate.equals(locale.getLanguage()));
            }
        }
        return new Resolved("", DEFAULT.getLanguage(), true);
    }

    /** The text exactly for the language of {@code locale}, if there is one. */
    public Optional<String> get(Locale locale) {
        return Optional.ofNullable(values.get(locale.getLanguage()))
                .filter(text -> !text.isBlank());
    }

    /** A copy with the text for the language of {@code locale} replaced. */
    public LocalizedText with(Locale locale, String text) {
        Map<String, String> copy = new LinkedHashMap<>(values);
        if (text.isBlank()) {
            copy.remove(locale.getLanguage());
        } else {
            copy.put(locale.getLanguage(), text.strip());
        }
        return new LocalizedText(copy);
    }
}
