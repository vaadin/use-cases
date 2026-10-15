package com.example.pdfviewer;

import java.util.ArrayList;
import java.util.List;

import com.example.documents.SampleDocuments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfViewerTest extends BrowserlessTest {

    private PdfViewer viewer;
    private PdfViewerTester tester;

    @BeforeEach
    void addViewer() {
        viewer = new PdfViewer();
        UI.getCurrent().add(viewer);
        tester = new PdfViewerTester(viewer);
    }

    @Test
    void pageCountComesFromTheLoadedDocument() {
        viewer.setSrc(SampleDocuments.priceList().handler());
        assertEquals(0, viewer.getPageCount(), "Nothing has loaded yet");

        tester.load(4, "Price List 2026");
        assertEquals(4, viewer.getPageCount());

        viewer.setSrc(SampleDocuments.travelPolicy().handler());
        assertEquals(0, viewer.getPageCount(),
                "A new document has not loaded yet");
        tester.fail("network");
        assertEquals(0, viewer.getPageCount());
    }

    @Test
    void scrollingUpdatesThePageAndTellsListenersItCameFromTheUser() {
        List<String> events = new ArrayList<>();
        viewer.addPageChangeListener(event -> events.add(event.getPage()
                + (event.isFromClient() ? " user" : " server")));

        tester.scrollTo(3);
        viewer.setPage(5);

        assertEquals(5, viewer.getPage());
        assertEquals(List.of("3 user", "5 server"), events);
    }

    @Test
    void errorReasonsAreTyped() {
        List<PdfViewer.ErrorReason> reasons = new ArrayList<>();
        viewer.addDocumentErrorListener(
                event -> reasons.add(event.getReason()));

        tester.fail("network");
        tester.fail("invalid");
        tester.fail("password");

        assertEquals(List.of(PdfViewer.ErrorReason.NETWORK,
                PdfViewer.ErrorReason.INVALID, PdfViewer.ErrorReason.PASSWORD),
                reasons);
    }

    @Test
    void documentsAreServedInlineSoTheyAreNotDownloads() {
        var handler = SampleDocuments.priceList().handler();
        viewer.setSrc(handler);
        assertTrue(handler.isInline());
        assertTrue(viewer.getElement().getAttribute("src")
                .endsWith("price-list-2026.pdf"));
    }

    @Test
    void printIsSentToTheBrowser() {
        tester.printRequested();
        assertFalse(tester.printRequested());
        viewer.print();
        assertTrue(tester.printRequested());
    }
}
