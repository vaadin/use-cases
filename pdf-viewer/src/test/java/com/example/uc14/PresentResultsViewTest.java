package com.example.uc14;

import java.util.List;

import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.pdfviewer.Zoom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PresentResultsView.class)
class PresentResultsViewTest extends SpringBrowserlessTest {

    private ExecutedJavaScript executed;

    @BeforeEach
    void captureJavaScript() {
        executed = ExecutedJavaScript.capture();
    }

    @Test
    void theViewerShowsTheSlidesFittedWithoutItsHeader() {
        navigate(PresentResultsView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();

        assertFalse(viewer.isHeaderVisible());
        assertEquals(Zoom.PAGE_FIT, viewer.getZoom());
        assertTrue(viewer.getSrc().endsWith("q3-2026-results.pdf"),
                viewer.getSrc());
    }

    @Test
    void theAppsButtonsMoveBetweenSlidesAndFollowThePage() {
        navigate(PresentResultsView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single(), executed);
        assertEquals("Loading…", position());
        assertFalse(button("Next").isEnabled());

        viewer.load(5, "Q3 2026 Results");
        assertEquals("Slide 1 of 5", position());
        assertFalse(button("Previous").isEnabled());

        test(button("Next")).click();
        assertEquals(List.of(2), viewer.takeScrollToPage());
        viewer.scrollTo(2);
        assertEquals("Slide 2 of 5", position());

        test(button("Previous")).click();
        assertEquals(List.of(1), viewer.takeScrollToPage());

        viewer.scrollTo(5);
        assertEquals("Slide 5 of 5", position());
        assertFalse(button("Next").isEnabled());
        assertTrue(button("Previous").isEnabled());
    }

    @Test
    void theAppsDownloadButtonDownloadsTheLoadedSlides() {
        navigate(PresentResultsView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single(), executed);
        assertFalse(downloadButton().isEnabled());

        viewer.load(5, "Q3 2026 Results");
        test(downloadButton()).click();

        assertEquals(1, viewer.takeDownloads());
    }

    private Button downloadButton() {
        return findInView(Button.class).all().stream().filter(
                button -> button.getAriaLabel().orElse("").equals("Download"))
                .findFirst().orElseThrow();
    }

    private Button button(String text) {
        return findInView(Button.class).withText(text).single();
    }

    private String position() {
        return findInView(Span.class).withClassName("uc14-position").single()
                .getText();
    }
}
