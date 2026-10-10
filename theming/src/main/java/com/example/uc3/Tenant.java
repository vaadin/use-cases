package com.example.uc3;

import com.example.Colors;

/**
 * A customer of the application and its styling, as it would come from a
 * database. Everything that ends up in the generated stylesheet is validated
 * here, so customer data can never inject arbitrary CSS.
 */
public record Tenant(String id, String name, String accentLight,
        String accentDark, String background, int radius, Font font) {

    /** The fonts a customer can choose from. */
    public enum Font {
        SANS("system-ui, sans-serif"),
        SERIF("Georgia, \"Times New Roman\", serif"),
        ROUNDED("ui-rounded, \"Nunito\", system-ui, sans-serif");

        private final String stack;

        Font(String stack) {
            this.stack = stack;
        }

        public String stack() {
            return stack;
        }
    }

    public Tenant {
        if (!id.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Invalid tenant id: " + id);
        }
        for (String color : new String[] { accentLight, accentDark,
                background }) {
            if (!Colors.isHex(color)) {
                throw new IllegalArgumentException("Invalid color: " + color);
            }
        }
        if (radius < 0 || radius > 24) {
            throw new IllegalArgumentException("Invalid radius: " + radius);
        }
    }

    /** The first letters of the name, for the logo placeholder. */
    public String initials() {
        StringBuilder initials = new StringBuilder();
        for (String word : name.split("\\s+")) {
            initials.append(word.charAt(0));
        }
        return initials.toString();
    }

    /** The stylesheet that turns the base theme into this customer's. */
    public String styleSheet() {
        return """
                /* %s */
                :root {
                    --aura-accent-color-light: %s;
                    --aura-accent-color-dark: %s;
                    --aura-background-color-light: %s;
                    --aura-base-radius: %d;
                    --aura-font-family: %s;
                    --lumo-primary-color: %s;
                    --lumo-primary-text-color: %s;
                    --lumo-border-radius-m: %dpx;
                    --lumo-font-family: %s;
                    --tenant-color: light-dark(%s, %s);
                }
                """.formatted(name.replace("*/", ""), accentLight, accentDark,
                background, radius, font.stack(), accentLight, accentLight,
                radius * 2, font.stack(), accentLight, accentDark);
    }
}
