package com.example.uc16;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PointerTrackerView.class)
class PointerTrackerViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithPanelAndCoordinateSpans() {
        navigate(PointerTrackerView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC16 — Pointer tracker".equals(h.getText())));
        assertNotNull(findInView(Div.class).id("panel"));
        assertNotNull(findInView(Span.class).id("x"));
        assertNotNull(findInView(Span.class).id("y"));
    }
}
