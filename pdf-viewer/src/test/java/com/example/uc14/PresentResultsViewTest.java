package com.example.uc14;

import java.util.List;

import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.pdfviewer.PdfViewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PresentResultsView.class)
class PresentResultsViewTest extends SpringBrowserlessTest {

    @Test
    void theAppsButtonsMoveBetweenSlidesAndFollowThePage() {
        navigate(PresentResultsView.class);
        PdfViewer pdfViewer = findInView(PdfViewer.class).single();
        assertFalse(pdfViewer.isHeaderVisible());
        PdfViewerTester viewer = new PdfViewerTester(pdfViewer);
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

    private Button button(String text) {
        return findInView(Button.class).withText(text).single();
    }

    private String position() {
        return findInView(Span.class).withClassName("uc14-position").single()
                .getText();
    }
}
