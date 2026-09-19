package com.example.uc2;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OpenInNewTabView.class)
class OpenInNewTabViewTest extends SpringBrowserlessTest {

    @Test
    void theLinkOpensTheDocumentItselfInANewTab() {
        navigate(OpenInNewTabView.class);

        Anchor view = findInView(Anchor.class).id("view-link");
        assertEquals("_blank", view.getTarget().orElseThrow());
        assertFalse(view.getHref().isEmpty());
    }

    @Test
    void theServerHasToGoThroughAHiddenAnchorToOpenTheSameDocument() {
        navigate(OpenInNewTabView.class);

        test(findInView(Button.class).id("open-button")).click();

        assertFalse(
                findInView(Span.class).id("resolved-url").getText().isEmpty(),
                "The URL only becomes visible through Anchor#getHref()");
        assertTrue(
                Objects.requireNonNull(UI.getCurrent()).getInternals()
                        .containsPendingJavascript("this.click()"),
                "Page#open cannot take a DownloadHandler, so the anchor is "
                        + "clicked from JavaScript instead");
    }
}
