package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;

/**
 * Moves the fullscreen state of the current UI, as the browser would.
 * <p>
 * Browserless tests have no fullscreen simulator (API-GAPS.md #3), so this
 * calls {@link Fullscreen#setStateFromClient(UI, String)}, the bootstrap entry
 * point that is not meant for application code. Replace it with the simulator
 * once browserless-test has one.
 */
public final class FullscreenTestSupport {

    private FullscreenTestSupport() {
    }

    public static void setFullscreenState(FullscreenState state) {
        Fullscreen.setStateFromClient(UI.getCurrent(), state.name());
    }
}
