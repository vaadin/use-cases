package com.example.uc4;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShareListItemsView.class)
class ShareListItemsViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersOneRowPerArticle() {
        navigate(ShareListItemsView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC4 — Share each item in a list".equals(h.getText())));

        long rowCount = findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("share-list-item"))
                .count();
        // The view ships with three articles.
        assertEquals(3, rowCount);
    }

    @Test
    void everyRowHasItsOwnShareButton() {
        navigate(ShareListItemsView.class);
        runPendingSignalsTasks();

        long shareButtons = findInView(Button.class).all().stream()
                .filter(b -> b.getElement().getAttribute("aria-label") != null
                        && b.getElement().getAttribute("aria-label")
                                .startsWith("Share \""))
                .count();
        assertEquals(3, shareButtons);
    }

    @Test
    void shareButtonsEnableTogetherWithSupportSignal() {
        navigate(ShareListItemsView.class);
        runPendingSignalsTasks();

        assertAllShareButtonsEnabled(false);

        WebShareTestSupport.setSupport(WebShareSupport.SUPPORTED);
        runPendingSignalsTasks();
        assertAllShareButtonsEnabled(true);

        WebShareTestSupport.setSupport(WebShareSupport.UNSUPPORTED);
        runPendingSignalsTasks();
        assertAllShareButtonsEnabled(false);
    }

    private void assertAllShareButtonsEnabled(boolean expected) {
        assertTrue(
                findInView(Button.class).all().stream().filter(
                        b -> b.getElement().getAttribute("aria-label") != null
                                && b.getElement().getAttribute("aria-label")
                                        .startsWith("Share \""))
                        .allMatch(b -> b.isEnabled() == expected),
                "expected all per-row share buttons enabled=" + expected);
    }
}
