package com.example.uc2;

import java.util.List;

import com.example.ScreenOrientationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = OrientationViewerView.class)
class OrientationViewerViewTest extends SpringBrowserlessTest {

    @Test
    void readoutsArrowAndBadgeTrackTheOrientation() {
        navigate(OrientationViewerView.class);
        runPendingSignalsTasks();

        assertEquals(List.of("UNKNOWN", "0°", "false", "false"), values());
        assertEquals("Waiting for the browser to report…", badge());

        setOrientation(ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        assertEquals(List.of("LANDSCAPE_PRIMARY", "90°", "true", "false"),
                values());
        assertEquals("Supported — current type: landscape-primary", badge());
        assertEquals("rotate(90deg)", arrowTransform());

        setOrientation(ScreenOrientationType.PORTRAIT_SECONDARY, 180);
        assertEquals(List.of("PORTRAIT_SECONDARY", "180°", "false", "true"),
                values());
        assertEquals("rotate(180deg)", arrowTransform());

        setOrientation(ScreenOrientationType.UNSUPPORTED, 0);
        assertEquals("Screen Orientation API not supported", badge());
    }

    private List<String> values() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc2-table")).findFirst()
                .orElseThrow().getChildren().map(c -> (Span) c)
                .filter(s -> !s.hasClassName("uc2-label")).map(Span::getText)
                .toList();
    }

    private String badge() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("status-badge")).findFirst()
                .orElseThrow().getText();
    }

    private String arrowTransform() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc2-arrow")).findFirst()
                .orElseThrow().getStyle().get("transform");
    }

    private void setOrientation(ScreenOrientationType type, int angle) {
        ScreenOrientationTestSupport.setScreenOrientation(type, angle);
        runPendingSignalsTasks();
    }
}
