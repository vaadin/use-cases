package com.example.uc1;

import com.example.ScreenOrientationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AdaptiveLayoutView.class)
class AdaptiveLayoutViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndContainer() {
        navigate(AdaptiveLayoutView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC1 — Adaptive layout".equals(h.getText())));
        assertTrue(hasContainer(), "expected the uc1-container to be rendered");
    }

    @Test
    void containerReactsToOrientationChanges() {
        navigate(AdaptiveLayoutView.class);
        runPendingSignalsTasks();

        // Mock UI starts at UNKNOWN -> stacked.
        assertContainerClass("stacked");
        assertBadgeContains("Orientation unknown");

        setOrientation(ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        assertContainerClass("side-by-side");
        assertBadgeContains("Landscape");

        setOrientation(ScreenOrientationType.PORTRAIT_PRIMARY, 0);
        assertContainerClass("stacked");
        assertBadgeContains("Portrait");

        setOrientation(ScreenOrientationType.UNSUPPORTED, 0);
        assertBadgeContains("not supported");
    }

    private boolean hasContainer() {
        return findInView(Div.class).all().stream()
                .anyMatch(d -> d.getClassNames().stream()
                        .anyMatch(c -> c.equals("uc1-container")));
    }

    private void assertContainerClass(String cls) {
        assertTrue(
                findInView(Div.class).all().stream().filter(
                        d -> d.getClassNames().contains("uc1-container"))
                        .anyMatch(d -> d.getClassNames().contains(cls)),
                "expected uc1-container to carry class " + cls);
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected mode badge to contain \"" + fragment + "\"");
    }

    private void setOrientation(ScreenOrientationType type, int angle) {
        ScreenOrientationTestSupport.setScreenOrientation(type, angle);
        runPendingSignalsTasks();
    }
}
