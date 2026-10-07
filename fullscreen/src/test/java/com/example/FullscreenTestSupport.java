package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;

/**
 * Thin shim over {@link Fullscreen#setStateFromClient(UI, String)} — kept as a
 * separate type so test classes stay decoupled from the bootstrap-seeding entry
 * point should the API move again, and so a single import matches the
 * {@code PageVisibilityTestSupport} idiom used in the {@code page-visibility/}
 * module.
 */
public final class FullscreenTestSupport {

    private FullscreenTestSupport() {
    }

    public static void setFullscreenState(FullscreenState state) {
        Fullscreen.setStateFromClient(UI.getCurrent(), state.name());
    }
}
