package com.example.uc1;

import com.example.ScreenOrientationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AdaptiveLayoutView.class)
class AdaptiveLayoutViewTest extends SpringBrowserlessTest {

    @Test
    void panesSitSideBySideOnlyInLandscape() {
        navigate(AdaptiveLayoutView.class);
        runPendingSignalsTasks();

        // The browserless UI starts at UNKNOWN: stacked.
        assertFalse(sideBySide());
        assertEquals("Orientation unknown — defaulting to stacked", badge());

        setOrientation(ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        assertTrue(sideBySide());
        assertEquals("Landscape — side-by-side layout", badge());

        setOrientation(ScreenOrientationType.PORTRAIT_PRIMARY, 0);
        assertFalse(sideBySide());
        assertEquals("Portrait — stacked layout", badge());

        setOrientation(ScreenOrientationType.UNSUPPORTED, 0);
        assertFalse(sideBySide());
        assertEquals(
                "Screen Orientation API not supported — defaulting to stacked",
                badge());
    }

    private boolean sideBySide() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc1-container")).findFirst()
                .orElseThrow().hasClassName("uc1-side-by-side");
    }

    private String badge() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("status-badge")).findFirst()
                .orElseThrow().getText();
    }

    private void setOrientation(ScreenOrientationType type, int angle) {
        ScreenOrientationTestSupport.setScreenOrientation(type, angle);
        runPendingSignalsTasks();
    }
}
