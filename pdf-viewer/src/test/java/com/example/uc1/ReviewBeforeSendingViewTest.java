package com.example.uc1;

import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ReviewBeforeSendingView.class)
class ReviewBeforeSendingViewTest extends SpringBrowserlessTest {

    @Test
    void sendTurnsOnOnlyOnceTheInvoiceHasLoaded() {
        navigate(ReviewBeforeSendingView.class);
        Button send = sendButton();
        assertFalse(send.isEnabled(), "Nothing to look at yet");

        viewer().load(1, "Invoice 2026-0412");

        assertTrue(send.isEnabled());
        assertEquals("Draft, 1,325.00 EUR", status());
    }

    @Test
    void anInvoiceThatCanNotBeShownCanNotBeSent() {
        navigate(ReviewBeforeSendingView.class);
        viewer().fail("invalid");

        assertFalse(sendButton().isEnabled());
        assertTrue(status().contains("could not be shown"), status());
    }

    @Test
    void sendingMarksTheInvoiceAndAnotherInvoiceStartsOver() {
        navigate(ReviewBeforeSendingView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        String firstSrc = viewer.getElement().getAttribute("src");
        new PdfViewerTester(viewer).load(1, "");
        test(sendButton()).click();

        assertTrue(status().startsWith("Marked as sent to Nordwind"), status());
        assertFalse(sendButton().isEnabled(), "Sent once is enough");

        var second = SampleDocuments.INVOICES.get(1);
        test(findInView(Select.class).single())
                .selectItem(second.number() + " — " + second.customer());
        assertNotEquals(firstSrc, viewer.getElement().getAttribute("src"));
        assertFalse(sendButton().isEnabled(), "The new PDF has not loaded");
    }

    private PdfViewerTester viewer() {
        return new PdfViewerTester(findInView(PdfViewer.class).single());
    }

    private Button sendButton() {
        return findInView(Button.class).withText("Send to customer").single();
    }

    private String status() {
        return findInView(Span.class).withClassName("uc1-status").single()
                .getText();
    }
}
