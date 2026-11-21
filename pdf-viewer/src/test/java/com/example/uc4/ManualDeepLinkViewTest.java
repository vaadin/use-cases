package com.example.uc4;

import java.util.List;
import java.util.Map;

import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@BrowserlessTestConfig(javaScriptHandlers = PdfViewerTester.Browser.class)
@ViewPackages(classes = ManualDeepLinkView.class)
class ManualDeepLinkViewTest extends SpringBrowserlessTest {

    @Test
    void aLinkOpensTheGuideAtItsPage() {
        navigate("uc4?page=5", ManualDeepLinkView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single());

        assertEquals(List.of(5), viewer.takeScrollToPage());
        viewer.load(12, "");
        assertEquals(List.of(), viewer.takeScrollToPage(),
                "A page of the guide is not corrected");
        assertEquals("uc4?page=5", address());
    }

    @Test
    void eachHelpQuestionLinksToTheFirstPageOfItsChapter() {
        navigate(ManualDeepLinkView.class);
        Map<String, Integer> chapterPages = SampleDocuments.userGuide()
                .chapterPages();

        RouterLink link = findInView(RouterLink.class)
                .withText("How do I export a report to Excel?").single();

        assertEquals("uc4?page=" + chapterPages.get("Exporting data"),
                link.getHref());
    }

    @Test
    void scrollingWritesThePageBackIntoTheAddress() {
        navigate("uc4?page=2", ManualDeepLinkView.class);
        new PdfViewerTester(findInView(PdfViewer.class).single()).scrollTo(7);

        assertEquals("uc4?page=7", address());
    }

    @Test
    void aLinkPastTheEndOpensTheLastPage() {
        navigate("uc4?page=99", ManualDeepLinkView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single());
        assertEquals(List.of(99), viewer.takeScrollToPage());
        viewer.load(12, "");

        assertEquals(List.of(12), viewer.takeScrollToPage());
        assertEquals("uc4?page=12", address());
    }

    @Test
    void aPageThatIsNotANumberOpensTheFirstPage() {
        assertEquals(1, ManualDeepLinkView.parsePage("abc"));
        assertEquals(1, ManualDeepLinkView.parsePage("-3"));
        assertEquals(4, ManualDeepLinkView.parsePage(" 4 "));
    }

    private String address() {
        return findInView(Span.class).withClassName("uc4-address").single()
                .getText();
    }
}
