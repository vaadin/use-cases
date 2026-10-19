package com.example;

/**
 * Text sizes a user can pick. The value is Aura's unitless
 * {@code --aura-base-font-size}, from which every font size is derived.
 */
public enum FontSize {

    SMALL("Small", 13),
    DEFAULT("Default", 14),
    LARGE("Large", 16),
    EXTRA_LARGE("Extra large", 18);

    private final String title;
    private final int baseSize;

    FontSize(String title, int baseSize) {
        this.title = title;
        this.baseSize = baseSize;
    }

    public String title() {
        return title;
    }

    public int baseSize() {
        return baseSize;
    }
}
