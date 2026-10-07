package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.webshare.WebShareSupport;

/**
 * Shared helper for browserless tests that need to drive
 * {@link com.vaadin.flow.component.webshare.WebShare#supportSignal()}
 * transitions. The setter is on {@code UIInternals} (public,
 * framework-internal) — calling it through this helper keeps the call site
 * readable and gives us one place to revisit if the signal moves elsewhere.
 */
public final class WebShareTestSupport {

    private WebShareTestSupport() {
    }

    public static void setSupport(WebShareSupport state) {
        UI.getCurrent().getInternals().setWebShareSupport(state);
    }
}
