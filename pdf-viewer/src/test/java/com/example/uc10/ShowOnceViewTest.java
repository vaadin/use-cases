package com.example.uc10;

import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShowOnceView.class)
class ShowOnceViewTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theDocumentIsSentForTheFirstRequestOnly() {
        OneTimeDocument document = new OneTimeDocument();
        assertNull(document.openedAt());

        assertTrue(document.take());
        assertNotNull(document.openedAt());
        assertFalse(document.take(), "a reload or second tab gets nothing");

        document.reset();
        assertTrue(document.take());
    }

    @Test
    void openingTheBriefingShowsItAndWhenItWasOpened() {
        ShowOnceView view = navigate(ShowOnceView.class);
        assertFalse(viewer(view).isVisible());

        test(button("Open the briefing")).click();
        assertTrue(viewer(view).isVisible());
        assertFalse(button("Open the briefing").isEnabled());
        assertTrue(viewer(view).getElement().getAttribute("src")
                .endsWith("board-briefing-q3.pdf"));
        // The browser fetches the file, then the viewer reports it loaded.
        document().take();
        new PdfViewerTester(viewer(view)).load(2, "");

        assertTrue(status().startsWith("Opened at"), status());
    }

    @Test
    void aLaterVisitSaysTheBriefingWasOpenedAlready() {
        document().take();

        ShowOnceView view = navigate(ShowOnceView.class);

        assertFalse(button("Open the briefing").isEnabled());
        assertFalse(viewer(view).isVisible());
        assertTrue(status().startsWith("You opened the briefing at"), status());
    }

    @Test
    void theDemoControlsLetTheBriefingBeOpenedAgain() {
        document().take();
        navigate(ShowOnceView.class);

        test(button("Let me open it again")).click();

        assertTrue(button("Open the briefing").isEnabled());
        assertTrue(status().startsWith("Not opened yet"), status());
    }

    private OneTimeDocument document() {
        return context.getBean(OneTimeDocument.class);
    }

    private static PdfViewer viewer(ShowOnceView view) {
        return view.getChildren().filter(PdfViewer.class::isInstance)
                .map(PdfViewer.class::cast).findFirst().orElseThrow();
    }

    private Button button(String text) {
        return findInView(Button.class).withText(text).single();
    }

    private String status() {
        return findInView(Paragraph.class).withClassName("uc10-status").single()
                .getText();
    }
}
