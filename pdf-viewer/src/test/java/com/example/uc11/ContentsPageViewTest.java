package com.example.uc11;

import java.util.Map;

import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ContentsPageView.class)
class ContentsPageViewTest extends SpringBrowserlessTest {

    @Test
    void theBarShowsTheChapterOfTheCurrentPage() {
        Map<String, Integer> chapterPages = SampleDocuments.employeeHandbook()
                .chapterPages();
        navigate(ContentsPageView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single());
        assertEquals("Contents", chapter());

        viewer.scrollTo(chapterPages.get("Travel"));

        assertEquals("Travel", chapter());
    }

    @Test
    void backToContentsOpensTheFirstPage() {
        navigate(ContentsPageView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        new PdfViewerTester(viewer).scrollTo(4);

        test(findInView(Button.class).withText("Back to contents").single())
                .click();

        assertEquals(1, viewer.getPage());
        assertEquals("Contents", chapter());
    }

    private String chapter() {
        return findInView(Span.class).withClassName("uc11-chapter").single()
                .getText();
    }
}
