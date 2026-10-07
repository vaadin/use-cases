package com.example.uc1;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShareThisPageView.class)
class ShareThisPageViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndButton() {
        navigate(ShareThisPageView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC1 — Share this page".equals(h.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Share this page".equals(b.getText())));
    }

    @Test
    void shareButtonEnabledOnlyWhenSupported() {
        navigate(ShareThisPageView.class);
        runPendingSignalsTasks();

        // The mock UI starts at UNKNOWN; button must be disabled and the
        // badge must say so.
        assertBadgeContains("Detecting");
        assertButtonEnabled(false);

        setSupport(WebShareSupport.SUPPORTED);
        assertBadgeContains("Native sharing available");
        assertButtonEnabled(true);

        setSupport(WebShareSupport.UNSUPPORTED);
        assertBadgeContains("does not support");
        assertButtonEnabled(false);
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }

    private void assertButtonEnabled(boolean expected) {
        assertTrue(
                findInView(Button.class).all().stream()
                        .filter(b -> "Share this page".equals(b.getText()))
                        .anyMatch(b -> b.isEnabled() == expected),
                "expected Share button enabled=" + expected);
    }

    private void setSupport(WebShareSupport state) {
        WebShareTestSupport.setSupport(state);
        runPendingSignalsTasks();
    }
}
