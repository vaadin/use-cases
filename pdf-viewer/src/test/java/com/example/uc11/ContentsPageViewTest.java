package com.example.uc11;

import java.util.List;
import java.util.Map;

import com.example.documents.SampleDocuments;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ContentsPageView.class)
class ContentsPageViewTest extends SpringBrowserlessTest {

    private ExecutedJavaScript executed;

    @BeforeEach
    void captureJavaScript() {
        executed = ExecutedJavaScript.capture();
    }

    @Test
    void theBarShowsTheChapterOfTheCurrentPage() {
        Map<String, Integer> chapterPages = SampleDocuments.employeeHandbook()
                .chapterPages();
        navigate(ContentsPageView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single(), executed);
        assertEquals("Contents", chapter());

        viewer.scrollTo(chapterPages.get("Travel"));

        assertEquals("Travel", chapter());
    }

    @Test
    void backToContentsOpensTheFirstPage() {
        navigate(ContentsPageView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single(), executed);

        test(findInView(Button.class).withText("Back to contents").single())
                .click();

        assertEquals(List.of(1), viewer.takeScrollToPage());
    }

    private String chapter() {
        return findInView(Span.class).withClassName("uc11-chapter").single()
                .getText();
    }
}
