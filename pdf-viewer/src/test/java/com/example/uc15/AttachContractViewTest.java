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
    void aFileTheServerCanNotOpenIsTurnedAwayWithItsReason() {
        view = navigate(AttachContractView.class);

        upload("contract.pdf",
                "not a PDF at all".getBytes(StandardCharsets.UTF_8));
        assertFalse(viewer().isVisible());
        assertTrue(status().contains("not a PDF, or it is damaged"), status());

        Pdf payslip = SampleDocuments.payslip();
        upload(payslip.fileName(), payslip.bytes());
        assertFalse(viewer().isVisible());
        assertTrue(status().contains("protected with a password"), status());
        assertFalse(submit().isEnabled());
    }

    @Test
    void aFileTheViewerCanNotShowCanNotBeSubmitted() {
        view = navigate(AttachContractView.class);
        Pdf contract = SampleDocuments.serviceAgreement();
        upload(contract.fileName(), contract.bytes());

        new PdfViewerTester(viewer()).fail("invalid");

        assertFalse(viewer().isVisible());
        assertFalse(submit().isEnabled());
        assertEquals("The viewer can not show this file. Upload another copy.",
                status());
    }

    @Test
    void theDamagedSampleIsTurnedAwayByTheServer() {
        assertTrue(AttachContractView
                .problemOf(SampleDocuments.damagedReport().bytes())
                .contains("damaged"));
        assertEquals(null, AttachContractView
                .problemOf(SampleDocuments.serviceAgreement().bytes()));
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
