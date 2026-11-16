package com.example.uc15;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.upload.Upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AttachContractView.class)
class AttachContractViewTest extends SpringBrowserlessTest {

    private AttachContractView view;

    @Test
    void anUploadedPdfIsShownAndCanBeSubmittedOnceLoaded() {
        view = navigate(AttachContractView.class);
        Pdf contract = SampleDocuments.serviceAgreement();

        upload(contract.fileName(), contract.bytes());

        assertTrue(viewer().isVisible());
        assertTrue(viewer().getSrc().endsWith(contract.fileName()),
                viewer().getSrc());
        assertFalse(submit().isEnabled(), "Not before the viewer shows it");

        new PdfViewerTester(viewer()).load(4, "Master Service Agreement");
        assertTrue(submit().isEnabled());

        test(submit()).click();
        assertTrue(status().startsWith("Submitted " + contract.fileName()),
                status());
        assertFalse(submit().isEnabled());
    }

    @Test
    void aFileTheServerCanNotOpenReplacesTheShownOneWithItsReason() {
        view = navigate(AttachContractView.class);
        Pdf contract = SampleDocuments.serviceAgreement();
        upload(contract.fileName(), contract.bytes());
        new PdfViewerTester(viewer()).load(4, "Master Service Agreement");
        assertTrue(submit().isEnabled());

        Pdf damaged = SampleDocuments.damagedReport();
        upload(damaged.fileName(), damaged.bytes());
        assertFalse(viewer().isVisible());
        assertFalse(submit().isEnabled());
        assertTrue(status().contains("not a PDF, or it is damaged"), status());

        upload("contract.pdf",
                "not a PDF at all".getBytes(StandardCharsets.UTF_8));
        assertTrue(status().contains("not a PDF, or it is damaged"), status());

        Pdf payslip = SampleDocuments.payslip();
        upload(payslip.fileName(), payslip.bytes());
        assertFalse(viewer().isVisible());
        assertTrue(status().contains("protected with a password"), status());
    }

    @Test
    void aFileOverTheLimitIsTurnedAway() {
        view = navigate(AttachContractView.class);

        // The browser refuses it, and the view shows the upload's message.
        upload("scan.pdf", new byte[AttachContractView.MAX_FILE_BYTES + 1]);
        assertFalse(viewer().isVisible());
        assertEquals("File is Too Big.", status());

        // The server refuses it too, also when the request hides its size.
        assertTrue(AttachContractView
                .fitsLimit(AttachContractView.MAX_FILE_BYTES));
        assertFalse(AttachContractView
                .fitsLimit(AttachContractView.MAX_FILE_BYTES + 1L));
        assertFalse(AttachContractView.fitsLimit(-1));
    }

    @Test
    void aFileTheViewerCanNotShowCanNotBeSubmitted() {
        view = navigate(AttachContractView.class);
        Pdf contract = SampleDocuments.serviceAgreement();
        upload(contract.fileName(), contract.bytes());
        PdfViewerTester viewer = new PdfViewerTester(viewer());

        viewer.fail("invalid");
        assertFalse(viewer().isVisible());
        assertEquals("The viewer can not show this file. Upload another copy.",
                status());

        // A load that arrives after the error does not make it submittable.
        viewer.load(4, "Master Service Agreement");
        assertFalse(submit().isEnabled());
    }

    private void upload(String fileName, byte[] content) {
        test(findInView(Upload.class).single()).upload(fileName,
                "application/pdf", content);
    }

    /** The viewer, also while it is hidden, which {@code findInView} skips. */
    private PdfViewer viewer() {
        return descendants(view).filter(PdfViewer.class::isInstance)
                .map(PdfViewer.class::cast).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren()
                .flatMap(AttachContractViewTest::descendants));
    }

    private Button submit() {
        return findInView(Button.class).withText("Submit the contract")
                .single();
    }

    private String status() {
        return findInView(Paragraph.class).withClassName("uc15-status").single()
                .getText();
    }
}
