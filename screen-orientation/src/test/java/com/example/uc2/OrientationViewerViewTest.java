package com.example.uc2;

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
@ViewPackages(classes = OrientationViewerView.class)
class OrientationViewerViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndReadouts() {
        navigate(OrientationViewerView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC2 — Orientation viewer".equals(h.getText())));
        assertSpanText("UNKNOWN");
        assertSpanText("Waiting for client bootstrap");
    }

    @Test
    void readoutsTrackOrientationSignal() {
        navigate(OrientationViewerView.class);
        runPendingSignalsTasks();

        ScreenOrientationTestSupport.setScreenOrientation(
                ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        runPendingSignalsTasks();

        assertSpanText("LANDSCAPE_PRIMARY");
        assertSpanText("90°");
        assertSpanText("Supported — current type: landscape-primary");
        assertArrowRotation(90);

        ScreenOrientationTestSupport.setScreenOrientation(
                ScreenOrientationType.PORTRAIT_SECONDARY, 180);
        runPendingSignalsTasks();

        assertSpanText("PORTRAIT_SECONDARY");
        assertSpanText("180°");
        assertArrowRotation(180);

        ScreenOrientationTestSupport
                .setScreenOrientation(ScreenOrientationType.UNSUPPORTED, 0);
        runPendingSignalsTasks();
        assertSpanText("Screen Orientation API not supported");
    }

    private void assertSpanText(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected a span containing \"" + fragment + "\"");
    }

    private void assertArrowRotation(int degrees) {
        assertTrue(findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("uc2-arrow"))
                .anyMatch(d -> {
                    String transform = d.getStyle().get("transform");
                    return transform != null
                            && transform.contains(degrees + "deg");
                }),
                "expected uc2-arrow transform to contain " + degrees + "deg");
    }
}
