package com.example.uc1;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShareThisPageView.class)
class ShareThisPageViewTest extends SpringBrowserlessTest {

    @Test
    void shareButtonAndBadgeFollowTheSupportSignal() {
        navigate(ShareThisPageView.class);
        runPendingSignalsTasks();

        // The browserless UI starts at UNKNOWN.
        assertEquals("Detecting…", badge().getText());
        assertTrue(badge().hasClassName("uc1-unknown"));
        assertFalse(shareButton().isEnabled());

        setSupport(WebShareSupport.SUPPORTED);
        assertEquals("Native sharing available", badge().getText());
        assertFalse(badge().hasClassName("uc1-unknown"));
        assertTrue(shareButton().isEnabled());

        setSupport(WebShareSupport.UNSUPPORTED);
        assertEquals("This browser has no navigator.share", badge().getText());
        assertTrue(badge().hasClassName("uc1-unsupported"));
        assertFalse(shareButton().isEnabled());
    }

    private Span badge() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("uc1-status")).findFirst()
                .orElseThrow();
    }

    private Button shareButton() {
        return findInView(Button.class).all().stream()
                .filter(b -> "Share this page".equals(b.getText())).findFirst()
                .orElseThrow();
    }

    private void setSupport(WebShareSupport state) {
        WebShareTestSupport.setSupport(state);
        runPendingSignalsTasks();
    }
}
