package com.example;

import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * How the language of a session is chosen and remembered. Stored as a session
 * attribute so that UC2 can show where the current language came from.
 * <ol>
 * <li>Flow matches the browser's {@code Accept-Language} header against the
 * {@link SupportedLocales} when the session starts.</li>
 * <li>A language the user picked earlier, remembered in a cookie, overrides
 * that match.</li>
 * <li>Picking a language applies it to every tab of the session and remembers
 * it for the next visit.</li>
 * </ol>
 */
public final class LanguagePreference {

    static final String COOKIE = "locale";

    private static final int ONE_YEAR = 365 * 24 * 60 * 60;

    private final List<Locale> browserLocales;
    private final Locale matched;
    private final ValueSignal<@Nullable Locale> remembered;

    private LanguagePreference(List<Locale> browserLocales, Locale matched,
            @Nullable Locale remembered) {
        this.browserLocales = browserLocales;
        this.matched = matched;
        this.remembered = new ValueSignal<>(remembered);
    }

    static void init(VaadinSession session, VaadinRequest request) {
        Optional<Locale> remembered = fromCookies(request.getCookies());
        LanguagePreference preference = new LanguagePreference(
                Collections.list(request.getLocales()), session.getLocale(),
                remembered.orElse(null));
        session.setAttribute(LanguagePreference.class, preference);
        remembered.ifPresent(session::setLocale);
    }

    /**
     * The preference of the current session. A session that started before
     * {@link I18nConfig} was registered (or in a test) gets one that knows
     * nothing about the browser.
     */
    public static LanguagePreference current() {
        VaadinSession session = VaadinSession.getCurrent();
        LanguagePreference preference = session
                .getAttribute(LanguagePreference.class);
        if (preference == null) {
            preference = new LanguagePreference(List.of(), session.getLocale(),
                    null);
            session.setAttribute(LanguagePreference.class, preference);
        }
        return preference;
    }

    /** The languages the browser asked for, most preferred first. */
    public List<Locale> browserLocales() {
        return browserLocales;
    }

    /** The supported language Flow matched to the browser's request. */
    public Locale matched() {
        return matched;
    }

    /** The language remembered from an earlier choice, if any. */
    public Signal<@Nullable Locale> remembered() {
        return remembered.asReadonly();
    }

    /**
     * Switches every tab of the session to {@code locale} and remembers it for
     * the next visit.
     */
    public void choose(Locale locale) {
        VaadinSession.getCurrent().setLocale(locale);
        remembered.set(locale);
        // Gap: there is no server-side API for this, and with WebSocket push
        // there is no HTTP response to put a Set-Cookie header in.
        UI.getCurrent().getPage().executeJs(
                "document.cookie = $0 + '=' + $1 + ';path=/;max-age=' + $2"
                        + " + ';SameSite=Lax'",
                COOKIE, locale.toLanguageTag(), ONE_YEAR);
    }

    /**
     * Drops the remembered choice and goes back to the language matched from
     * the browser.
     */
    public void forget() {
        VaadinSession.getCurrent().setLocale(matched);
        remembered.set(null);
        UI.getCurrent().getPage().executeJs(
                "document.cookie = $0 + '=;path=/;max-age=0;SameSite=Lax'",
                COOKIE);
    }

    static Optional<Locale> fromCookies(Cookie @Nullable [] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE.equals(cookie.getName()))
                .map(cookie -> Locale.forLanguageTag(cookie.getValue()))
                .filter(SupportedLocales.ALL::contains).findFirst();
    }
}
