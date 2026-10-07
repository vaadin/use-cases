package com.example.uc5;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = IdleWarningView.class)
class IdleWarningViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithStatusBadge() {
        navigate(IdleWarningView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC5 — Idle warning".equals(h.getText())));
        assertEquals("active", findInView(Span.class).id("status").getText());
        assertNotNull(findInView(Button.class).id("reset"));
    }
}
