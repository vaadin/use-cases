package com.example.uc15;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LiveSizeReadoutView.class)
class LiveSizeReadoutViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithWidthAndHeightSpans() {
        navigate(LiveSizeReadoutView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC15 — Live size readout".equals(h.getText())));
        assertNotNull(findInView(Span.class).id("width"));
        assertNotNull(findInView(Span.class).id("height"));
    }
}
