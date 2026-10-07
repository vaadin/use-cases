package com.example.uc9;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ScrollIntoViewView.class)
class ScrollIntoViewViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithClientAndServerRows() {
        navigate(ScrollIntoViewView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC9 — Scroll into view".equals(h.getText())));
        // Client row (trigger API)
        assertNotNull(findInView(Button.class).id("client-a"));
        assertNotNull(findInView(Button.class).id("client-b"));
        assertNotNull(findInView(Button.class).id("client-c"));
        // Server row (round-trip via executeJs in click listener)
        assertNotNull(findInView(Button.class).id("server-a"));
        assertNotNull(findInView(Button.class).id("server-b"));
        assertNotNull(findInView(Button.class).id("server-c"));
        // Scroll targets
        assertNotNull(findInView(Div.class).id("section-a"));
        assertNotNull(findInView(Div.class).id("section-b"));
        assertNotNull(findInView(Div.class).id("section-c"));
    }
}
