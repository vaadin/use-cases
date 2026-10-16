package com.example.components;

import com.vaadin.flow.component.shared.ThemeVariant;

/** Theme variants of {@link RatingStars} (UC8). */
public enum RatingStarsVariant implements ThemeVariant {

    SMALL("small"), LARGE("large"), READ_ONLY_COMPACT("compact");

    private final String variant;

    RatingStarsVariant(String variant) {
        this.variant = variant;
    }

    @Override
    public String getVariantName() {
        return variant;
    }
}
