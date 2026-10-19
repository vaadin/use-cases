package com.example.uc3;

import com.example.ScreenOrientationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RotatePromptView.class)
class RotatePromptViewTest extends SpringBrowserlessTest {

    @Test
    void overlayCoversTheContentUntilTheDeviceIsLandscape() {
        navigate(RotatePromptView.class);
        runPendingSignalsTasks();

        // Required = landscape; orientation UNKNOWN: no overlay.
        assertFalse(overlayShown());

        setOrientation(ScreenOrientationType.PORTRAIT_PRIMARY, 0);
        assertTrue(overlayShown());
        assertEquals("Please rotate to landscape mode.", overlayMessage());
        assertEquals("Currently portrait, rotate to landscape", status());

        setOrientation(ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        assertFalse(overlayShown());
        assertEquals("Landscape OK", status());
    }

    @Test
    void pickingPortraitMovesTheOverlayToLandscape() {
        navigate(RotatePromptView.class);
        setOrientation(ScreenOrientationType.PORTRAIT_PRIMARY, 0);
        assertTrue(overlayShown());

        test(findInView(RadioButtonGroup.class).single())
                .selectItem("portrait");
        runPendingSignalsTasks();
        assertFalse(overlayShown());
        assertEquals("Portrait OK", status());

        setOrientation(ScreenOrientationType.LANDSCAPE_SECONDARY, 270);
        assertTrue(overlayShown());
        assertEquals("Please rotate to portrait mode.", overlayMessage());
    }

    @Test
    void unsupportedPlatformDoesNotBlockUi() {
        navigate(RotatePromptView.class);
        setOrientation(ScreenOrientationType.UNSUPPORTED, 0);

        assertFalse(overlayShown());
        assertEquals("Screen Orientation API not supported — overlay disabled",
                status());
    }

    private Div overlay() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc3-overlay")).findFirst()
                .orElseThrow();
    }

    private boolean overlayShown() {
        return !overlay().hasClassName("uc3-hidden");
    }

    private String overlayMessage() {
        return overlay().getChildren().map(c -> (Span) c)
                .reduce((first, second) -> second).orElseThrow().getText();
    }

    private String status() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("status-badge")).findFirst()
                .orElseThrow().getText();
    }

    private void setOrientation(ScreenOrientationType type, int angle) {
        ScreenOrientationTestSupport.setScreenOrientation(type, angle);
        runPendingSignalsTasks();
    }
}
