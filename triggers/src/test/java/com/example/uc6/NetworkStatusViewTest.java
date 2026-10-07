package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = NetworkStatusView.class)
class NetworkStatusViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithStatusBadge() {
        navigate(NetworkStatusView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC6 — Network status".equals(h.getText())));
        assertNotNull(findInView(Span.class).id("status"));
    }
}
