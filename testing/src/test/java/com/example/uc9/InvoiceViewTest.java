package com.example.uc9;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The view itself; the screenshot tests are the *ScreenshotIT classes. */
@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = InvoiceView.class)
class InvoiceViewTest extends SpringBrowserlessTest {

    @Test
    void rendersTheInvoiceWithItsVolatilePartMarked() {
        navigate(InvoiceView.class);

        assertEquals("Invoice INV-2026-0042",
                findInView(H2.class).single().getText());
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.hasClassName("volatile")
                        && s.getText().startsWith("Printed at ")));
    }
}
