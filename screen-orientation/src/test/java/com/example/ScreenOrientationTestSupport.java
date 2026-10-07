package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

/**
 * Shared helper for browserless tests that need to drive the screen orientation
 * signal on {@link ScreenOrientation#orientationSignal()}. In production the JS
 * bridge feeds the value through {@code UIInternals}; tests call the same
 * public seeding method directly.
 */
public final class ScreenOrientationTestSupport {

    private ScreenOrientationTestSupport() {
    }

    public static void setScreenOrientation(ScreenOrientationType orientation,
            int angle) {
        UI.getCurrent().getInternals().setScreenOrientationFromClient(
                orientation.getClientValue(), Integer.toString(angle));
    }
}
