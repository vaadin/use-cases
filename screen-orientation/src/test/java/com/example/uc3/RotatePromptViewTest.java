package com.example.uc3;

import com.example.ScreenOrientationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RotatePromptView.class)
class RotatePromptViewTest extends SpringBrowserlessTest {

    @Test
    void viewRenders() {
        navigate(RotatePromptView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().equals("UC3 — Rotate-your-device overlay")));
        assertTrue(hasOverlay());
        assertTrue(findInView(RadioButtonGroup.class).all().size() >= 1,
                "expected the required-orientation picker");
    }

    @Test
    void overlayHidesWhenRequiredLandscapeAndDeviceLandscape() {
        navigate(RotatePromptView.class);
        runPendingSignalsTasks();

        // Default required = LANDSCAPE; orientation UNKNOWN — overlay hidden.
        assertOverlayHidden(true);

        ScreenOrientationTestSupport.setScreenOrientation(
                ScreenOrientationType.PORTRAIT_PRIMARY, 0);
        runPendingSignalsTasks();
        // Portrait while landscape is required — overlay must show.
        assertOverlayHidden(false);
        assertStatusContains("rotate to landscape");

        ScreenOrientationTestSupport.setScreenOrientation(
                ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        runPendingSignalsTasks();
        // Landscape — overlay hides.
        assertOverlayHidden(true);
        assertStatusContains("Landscape OK");
    }

    @Test
    void unsupportedPlatformDoesNotBlockUi() {
        navigate(RotatePromptView.class);
        runPendingSignalsTasks();

        ScreenOrientationTestSupport
                .setScreenOrientation(ScreenOrientationType.UNSUPPORTED, 0);
        runPendingSignalsTasks();

        assertOverlayHidden(true);
        assertStatusContains("not supported");
    }

    private boolean hasOverlay() {
        return findInView(Div.class).all().stream()
                .anyMatch(d -> d.getClassNames().contains("uc3-overlay"));
    }

    private void assertOverlayHidden(boolean hidden) {
        boolean carriesHiddenClass = findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("uc3-overlay"))
                .anyMatch(d -> d.getClassNames().contains("hidden"));
        if (hidden) {
            assertTrue(carriesHiddenClass,
                    "expected overlay to carry the 'hidden' class");
        } else {
            assertTrue(!carriesHiddenClass,
                    "expected overlay NOT to carry the 'hidden' class");
        }
    }

    private void assertStatusContains(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected status badge text containing \"" + fragment + "\"");
    }
}
