package com.example;

import jakarta.servlet.http.Cookie;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * How the application looks for one user: the base theme, light or dark, the
 * brand or customer styling and the user's own adjustments. Stored in the
 * session and applied to every open tab by {@link AppearanceSetup}; the user's
 * own choices are also remembered in cookies for the next visit.
 */
public final class Appearance {

    static final String THEME = "theme";
    static final String COLOR_SCHEME = "color-scheme";
    static final String ACCENT = "accent";
    static final String FONT_SIZE = "font-size";
    static final String HIGH_CONTRAST = "high-contrast";
    static final String REDUCE_MOTION = "reduce-motion";

    private final ValueSignal<BaseTheme> theme;
    private final ValueSignal<ColorScheme.Value> colorScheme;
    private final ValueSignal<Boolean> brand = new ValueSignal<>(false);
    private final ValueSignal<@Nullable String> tenant = new ValueSignal<>(
            null);
    private final ValueSignal<@Nullable String> accent;
    private final ValueSignal<FontSize> fontSize;
    private final ValueSignal<Boolean> highContrast;
    private final ValueSignal<Boolean> reduceMotion;

    private Appearance(Map<String, String> remembered) {
        theme = new ValueSignal<>(
                parse(BaseTheme.class, remembered.get(THEME), BaseTheme.AURA));
        colorScheme = new ValueSignal<>(parse(ColorScheme.Value.class,
                remembered.get(COLOR_SCHEME), ColorScheme.Value.SYSTEM));
        String color = remembered.get(ACCENT);
        accent = new ValueSignal<>(
                color != null && Colors.isHex(color) ? color : null);
        fontSize = new ValueSignal<>(parse(FontSize.class,
                remembered.get(FONT_SIZE), FontSize.DEFAULT));
        highContrast = new ValueSignal<>(
                "true".equals(remembered.get(HIGH_CONTRAST)));
        reduceMotion = new ValueSignal<>(
                "true".equals(remembered.get(REDUCE_MOTION)));
    }

    static void init(VaadinSession session, VaadinRequest request) {
        session.setAttribute(Appearance.class,
                new Appearance(cookies(request.getCookies())));
    }

    /**
     * The appearance of the current session. A session that started before
     * {@link AppearanceSetup} was registered gets the defaults.
     */
    public static Appearance current() {
        VaadinSession session = VaadinSession.getCurrent();
        Appearance appearance = session.getAttribute(Appearance.class);
        if (appearance == null) {
            appearance = new Appearance(Map.of());
            session.setAttribute(Appearance.class, appearance);
        }
        return appearance;
    }

    public Signal<BaseTheme> theme() {
        return theme.asReadonly();
    }

    public void setTheme(BaseTheme value) {
        theme.set(value);
        MissingAPI.writeCookie(THEME, value.name());
    }

    public Signal<ColorScheme.Value> colorScheme() {
        return colorScheme.asReadonly();
    }

    public void setColorScheme(ColorScheme.Value value) {
        colorScheme.set(value);
        MissingAPI.writeCookie(COLOR_SCHEME, value.name());
    }

    /** Whether the application's own brand stylesheet is applied (UC1). */
    public Signal<Boolean> brand() {
        return brand.asReadonly();
    }

    public void setBrand(boolean value) {
        brand.set(value);
    }

    /** The customer whose styling is applied, if any (UC3). */
    public Signal<@Nullable String> tenant() {
        return tenant.asReadonly();
    }

    public void setTenant(@Nullable String value) {
        tenant.set(value);
    }

    /** The user's own accent color as {@code #rrggbb}, if any (UC4). */
    public Signal<@Nullable String> accent() {
        return accent.asReadonly();
    }

    public void setAccent(@Nullable String value) {
        accent.set(value);
        MissingAPI.writeCookie(ACCENT, value == null ? "" : value);
    }

    public Signal<FontSize> fontSize() {
        return fontSize.asReadonly();
    }

    public void setFontSize(FontSize value) {
        fontSize.set(value);
        MissingAPI.writeCookie(FONT_SIZE, value.name());
    }

    public Signal<Boolean> highContrast() {
        return highContrast.asReadonly();
    }

    public void setHighContrast(boolean value) {
        highContrast.set(value);
        MissingAPI.writeCookie(HIGH_CONTRAST, String.valueOf(value));
    }

    public Signal<Boolean> reduceMotion() {
        return reduceMotion.asReadonly();
    }

    public void setReduceMotion(boolean value) {
        reduceMotion.set(value);
        MissingAPI.writeCookie(REDUCE_MOTION, String.valueOf(value));
    }

    static Map<String, String> cookies(Cookie @Nullable [] cookies) {
        if (cookies == null) {
            return Map.of();
        }
        return Arrays.stream(cookies)
                .collect(Collectors.toMap(Cookie::getName,
                        cookie -> URLDecoder.decode(cookie.getValue(),
                                StandardCharsets.UTF_8),
                        (first, second) -> first));
    }

    private static <E extends Enum<E>> E parse(Class<E> type,
            @Nullable String name, E fallback) {
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equals(name)).findFirst()
                .orElse(fallback);
    }

}
