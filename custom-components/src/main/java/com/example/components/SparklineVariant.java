package com.example.components;

import com.vaadin.flow.component.shared.ThemeVariant;

/** Theme variants of {@link Sparkline} (UC8). */
public enum SparklineVariant implements ThemeVariant {

    SUCCESS("success"), ERROR("error"), FILLED("filled");

    private final String variant;

    SparklineVariant(String variant) {
        this.variant = variant;
    }

    @Override
    public String getVariantName() {
        return variant;
    }
}
