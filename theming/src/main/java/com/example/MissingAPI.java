package com.example;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Shims for theming that Flow does not provide yet. Each method names the gap
 * it covers; see API-GAPS.md for the full story and the API we would rather
 * call.
 */
public final class MissingAPI {

    private static final int ONE_YEAR = 365 * 24 * 60 * 60;

    private MissingAPI() {
    }

    /**
     * Whether a CSS media query matches in the browser, such as
     * {@code (prefers-color-scheme: dark)} or
     * {@code (prefers-reduced-motion: reduce)}. The value is {@code null} until
     * the browser has answered, and follows later changes of the operating
     * system setting.
     * <p>
     * Gap: the server cannot see the user's color scheme, contrast or motion
     * preferences. {@code ExtendedClientDetails#getColorScheme} only returns
     * what the application itself set. Migrate to built-in signals for these
     * preferences once Flow has them.
     */
    public static Signal<@Nullable Boolean> mediaQuery(Component owner,
            String query) {
        ValueSignal<@Nullable Boolean> matches = new ValueSignal<>(null);
        String event = mediaQueryEvent(query);
        owner.getElement()
                .addEventListener(event, e -> matches
                        .set(e.getEventData().get("event.detail").asBoolean()))
                .addEventData("event.detail");
        owner.getElement()
                .addAttachListener(e -> owner.getElement().executeJs("""
                        const query = matchMedia($0);
                        const send = () => this.dispatchEvent(
                            new CustomEvent($1, { detail: query.matches }));
                        query.addEventListener('change', send);
                        send();""", query, event));
        return matches.asReadonly();
    }

    /** The DOM event {@link #mediaQuery} listens to for a query. */
    public static String mediaQueryEvent(String query) {
        return "media-query-"
                + Integer.toHexString(query.hashCode() & Integer.MAX_VALUE);
    }

    /**
     * Sets CSS custom properties on the document's root element, where the
     * themes define their tokens; a {@code null} value removes the property.
     * <p>
     * Gap: {@code UI#getElement()} is the {@code <body>}. Tokens that the
     * themes compute from other tokens on {@code :root} (such as Aura's accent
     * color) do not pick up an override set on a descendant, so the override
     * has to go on {@code <html>}, which the server cannot reach. Migrate to a
     * server-side handle for the root element once there is one.
     */
    public static void setRootProperties(UI ui,
            Map<String, @Nullable String> properties) {
        ObjectNode values = JsonNodeFactory.instance.objectNode();
        properties.forEach((name, value) -> {
            if (value == null) {
                values.putNull(name);
            } else {
                values.put(name, value);
            }
        });
        ui.getPage().executeJs("""
                const style = document.documentElement.style;
                for (const [name, value] of Object.entries($0)) {
                  if (value === null) {
                    style.removeProperty(name);
                  } else {
                    style.setProperty(name, value);
                  }
                }""", values);
        Map<String, String> applied = new HashMap<>(rootProperties(ui));
        properties.forEach((name, value) -> {
            if (value == null) {
                applied.remove(name);
            } else {
                applied.put(name, value);
            }
        });
        ComponentUtil.setData(ui, RootProperties.class,
                new RootProperties(Map.copyOf(applied)));
    }

    /** The root properties {@link #setRootProperties} has set in a UI. */
    public static Map<String, String> rootProperties(UI ui) {
        RootProperties properties = ComponentUtil.getData(ui,
                RootProperties.class);
        return properties == null ? Map.of() : properties.values();
    }

    private record RootProperties(Map<String, String> values) {
    }

    /**
     * Remembers a value in a cookie for a year.
     * <p>
     * Gap: Flow has no API to persist a user's appearance choices, and with
     * WebSocket push there is no HTTP response to put a {@code Set-Cookie}
     * header in.
     */
    public static void writeCookie(String name, String value) {
        UI.getCurrent().getPage()
                .executeJs("document.cookie = $0 + '=' + encodeURIComponent($1)"
                        + " + ';path=/;max-age=' + $2 + ';SameSite=Lax'", name,
                        value, ONE_YEAR);
    }
}
