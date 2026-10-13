package com.example.uc9;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The view itself; the screenshot tests are the *ScreenshotIT classes. */
@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = InvoiceView.class)
class InvoiceViewTest extends SpringBrowserlessTest {

    @Test
    void rendersTheInvoiceWithItsVolatilePartMarked() {
        navigate(InvoiceView.class);

        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "Invoice INV-2026-0042".equals(h.getText())));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.hasClassName("volatile")
                        && s.getText().startsWith("Printed at ")));
    }

    @Test
    void showsTheReferenceImageTheTestsCompareWith() {
        navigate(InvoiceView.class);

        assertTrue(findInView(Image.class).all().stream().anyMatch(
                i -> "screenshots/invoice-playwright.png".equals(i.getSrc())));
        assertNotNull(getClass().getClassLoader().getResource(
                "META-INF/resources/screenshots/invoice-playwright.png"));
    }
}
