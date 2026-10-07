package com.example.acme;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Reads the payload of Observability Kit's insights endpoint
 * ({@code GET /actuator/vaadin/observability}, or the same Spring bean's
 * {@code section("observability")}), which is a plain map: an
 * {@code insights} list whose entries carry a {@code type}, a
 * {@code severity}, a {@code summary} sentence and an {@code evidence} map
 * whose keys depend on the type.
 */
public final class Insights {

    private Insights() {
    }

    /** The insights of a payload; empty when the payload has none. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> of(
            @Nullable Map<String, Object> payload) {
        return payload != null
                && payload.get("insights") instanceof List<?> list
                        ? (List<Map<String, Object>>) list
                        : List.of();
    }

    /**
     * Whether the kit had instrumentation registered when the payload was
     * produced. {@code false} means "nothing was watching" — in development
     * mode, typically no license key — which is a different answer from "no
     * findings", and a reader must be told which one they are looking at.
     */
    public static boolean isActive(@Nullable Map<String, Object> payload) {
        return payload != null
                && "active".equals(payload.get("instrumentation"));
    }

    /** An insight's evidence map; empty when it has none. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> evidenceOf(Map<String, Object> insight) {
        return insight.get("evidence") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    /** A payload value as display text, a dash when absent. */
    public static String text(@Nullable Object value) {
        return value == null ? "—" : value.toString();
    }

    /** The simple name of a fully qualified class name the kit reports. */
    public static String simpleName(String type) {
        int dot = type.lastIndexOf('.');
        return dot < 0 ? type : type.substring(dot + 1);
    }

    /**
     * The Aura accent utility class for an insight severity: it sets the
     * theme's accent tokens on the element, which the card and pill styles
     * pick up for border, surface and text.
     */
    public static String accentOf(String severity) {
        return switch (severity) {
        case "error" -> "v-error";
        case "warning" -> "v-warning";
        default -> "v-info";
        };
    }
}
