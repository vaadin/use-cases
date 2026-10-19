package com.example.uc10;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
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
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ViewPackages(classes = ShowOnceView.class)
class ShowOnceViewTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theDocumentIsSentForTheFirstRequestOnly() throws IOException {
        OneTimeDocument document = new OneTimeDocument();
        Pdf briefing = SampleDocuments.boardBriefing();
        DownloadHandler handler = document.handler(briefing);
        assertNull(document.openedAt());

        ByteArrayOutputStream first = new ByteArrayOutputStream();
        handler.handleDownloadRequest(request(first));
        assertArrayEquals(briefing.bytes(), first.toByteArray());
        assertNotNull(document.openedAt());

        ByteArrayOutputStream second = new ByteArrayOutputStream();
        DownloadEvent reload = request(second);
        handler.handleDownloadRequest(reload);
        verify(reload.getResponse()).setStatus(410);
        assertEquals(0, second.size(), "a reload or second tab gets nothing");

        document.reset();
        ByteArrayOutputStream again = new ByteArrayOutputStream();
        handler.handleDownloadRequest(request(again));
        assertArrayEquals(briefing.bytes(), again.toByteArray());
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

    private static DownloadEvent request(ByteArrayOutputStream body) {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(mock(VaadinRequest.class));
        when(event.getResponse()).thenReturn(mock(VaadinResponse.class));
        when(event.getOutputStream()).thenReturn(body);
        return event;
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
