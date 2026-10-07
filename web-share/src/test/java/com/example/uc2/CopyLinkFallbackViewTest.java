package com.example.uc2;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyLinkFallbackView.class)
class CopyLinkFallbackViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeading() {
        navigate(CopyLinkFallbackView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC2 — Share with copy-link fallback"
                        .equals(h.getText())));
    }

    @Test
    void slotSwapsBetweenShareAndCopyLink() {
        navigate(CopyLinkFallbackView.class);
        runPendingSignalsTasks();

        // UNKNOWN initial state: a disabled "Detecting…" button.
        assertButtonExists("Detecting…");

        setSupport(WebShareSupport.SUPPORTED);
        assertButtonExists("Share");
        assertButtonAbsent("Copy link");

        setSupport(WebShareSupport.UNSUPPORTED);
        assertButtonExists("Copy link");
        assertButtonAbsent("Share");
    }

    private void assertButtonExists(String text) {
        assertTrue(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> text.equals(b.getText())),
                "expected a button labelled \"" + text + "\"");
    }

    private void assertButtonAbsent(String text) {
        assertTrue(
                findInView(Button.class).all().stream()
                        .noneMatch(b -> text.equals(b.getText())),
                "expected no button labelled \"" + text + "\"");
    }

    private void setSupport(WebShareSupport state) {
        WebShareTestSupport.setSupport(state);
        runPendingSignalsTasks();
    }
}
