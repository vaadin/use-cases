package com.example.uc22;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = KeyEventLogView.class)
class KeyEventLogViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithAllReadoutSpans() {
        navigate(KeyEventLogView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC22 — Key event log".equals(h.getText())));
        for (String id : new String[] { "key", "code", "shift", "ctrl", "alt",
                "meta" }) {
            assertNotNull(findInView(Span.class).id(id),
                    "missing span id=" + id);
        }
    }
}
