package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = NetworkStatusView.class)
class NetworkStatusViewTest extends SpringBrowserlessTest {

    // The badge is updated in the browser only, because the server can not
    // be reached when it matters (API-GAPS.md #7), so the test checks what is
    // rendered.
    @Test
    void viewRendersAStatusBadgeForTheBrowserToFillIn() {
        navigate(NetworkStatusView.class);

        assertEquals("UC6 — Network status",
                findInView(H1.class).single().getText());
        Span badge = findInView(Span.class).id("status");
        assertEquals("…", badge.getText());
        assertEquals("uc6-status", badge.getClassName());
    }
}
