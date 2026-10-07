package com.example.uc6;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShareInviteLinkView.class)
class ShareInviteLinkViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndPlaceholders() {
        navigate(ShareInviteLinkView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC6 — Share an invite link".equals(h.getText())));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "—".equals(s.getText())));
    }

    @Test
    void generateProducesEightCharCode() {
        ShareInviteLinkView view = navigate(ShareInviteLinkView.class);
        runPendingSignalsTasks();

        clickGenerate();
        String first = view.currentCode();
        assertNotNull(first);
        assertEquals(8, first.length(), "expected 8-char code, was: " + first);
    }

    @Test
    void regenerateChangesTheCode() {
        ShareInviteLinkView view = navigate(ShareInviteLinkView.class);
        runPendingSignalsTasks();

        clickGenerate();
        String first = view.currentCode();
        clickGenerate();
        String second = view.currentCode();
        assertNotEquals(first, second,
                "regenerate should produce a new code each click");
    }

    @Test
    void shareButtonNeedsBothSupportAndAGeneratedCode() {
        ShareInviteLinkView view = navigate(ShareInviteLinkView.class);
        runPendingSignalsTasks();

        // Initial state: UNKNOWN + no code → disabled.
        assertShareButtonEnabled(false);

        WebShareTestSupport.setSupport(WebShareSupport.SUPPORTED);
        runPendingSignalsTasks();
        // SUPPORTED but no code yet → still disabled.
        assertShareButtonEnabled(false);

        clickGenerate();
        runPendingSignalsTasks();
        // SUPPORTED + code → enabled.
        assertShareButtonEnabled(true);

        WebShareTestSupport.setSupport(WebShareSupport.UNSUPPORTED);
        runPendingSignalsTasks();
        // Even with a code, going UNSUPPORTED disables again.
        assertShareButtonEnabled(false);
    }

    private void clickGenerate() {
        Button generate = findInView(Button.class).all().stream()
                .filter(b -> "Generate invite".equals(b.getText())).findFirst()
                .orElseThrow();
        generate.click();
    }

    private void assertShareButtonEnabled(boolean expected) {
        Button share = findInView(Button.class).all().stream()
                .filter(b -> "Share invite".equals(b.getText())).findFirst()
                .orElseThrow();
        if (expected) {
            assertTrue(share.isEnabled(),
                    "expected Share invite button enabled");
        } else {
            assertFalse(share.isEnabled(),
                    "expected Share invite button disabled");
        }
    }
}
