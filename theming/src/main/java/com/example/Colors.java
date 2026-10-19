package com.example;

import java.util.regex.Pattern;

/** Small helpers for {@code #rrggbb} colors. */
public final class Colors {

    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    private Colors() {
    }

    public static boolean isHex(String color) {
        return HEX.matcher(color).matches();
    }

    /**
     * The WCAG contrast ratio between two colors, from 1 (none) to 21 (black on
     * white). Text needs at least 4.5 to pass level AA, large text 3.
     */
    public static double contrastRatio(String first, String second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    /** The WCAG relative luminance of a color. */
    static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return 0.2126 * channel(rgb >> 16) + 0.7152 * channel(rgb >> 8)
                + 0.0722 * channel(rgb);
    }

    private static double channel(int value) {
        double c = (value & 0xff) / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
