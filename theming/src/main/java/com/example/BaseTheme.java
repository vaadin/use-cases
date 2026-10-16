package com.example;

import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;

/** The two themes Vaadin ships, each a single stylesheet. */
public enum BaseTheme {

    AURA("Aura", Aura.STYLESHEET), LUMO("Lumo", Lumo.STYLESHEET);

    private final String title;
    private final String styleSheet;

    BaseTheme(String title, String styleSheet) {
        this.title = title;
        this.styleSheet = styleSheet;
    }

    public String title() {
        return title;
    }

    public String styleSheet() {
        return styleSheet;
    }
}
