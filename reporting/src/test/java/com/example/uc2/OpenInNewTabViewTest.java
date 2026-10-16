package com.example.uc2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.example.data.Invoices;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OpenInNewTabView.class)
class OpenInNewTabViewTest extends SpringBrowserlessTest {

    @Test
    void theLinkOpensTheDocumentItselfInANewTab() throws IOException {
        navigate(OpenInNewTabView.class);

        Anchor view = findInView(Anchor.class).id("view-link");
        assertEquals("_blank", view.getTarget().orElseThrow());

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        test(view).download(pdf);
        try (PDDocument document = Loader.loadPDF(pdf.toByteArray())) {
            assertTrue(new PDFTextStripper().getText(document)
                    .contains("Invoice " + Invoices.sampleInvoice().number()));
        }
    }

    @Test
    void theButtonHasTheDocumentBoundForANewTab() {
        navigate(OpenInNewTabView.class);
        Button open = findInView(Button.class).id("open-button");

        // OpenInNewTab registers the handler on the button itself, under an
        // internal attribute; there is no browserless tester that fetches it
        // (API-GAPS.md #6).
        assertTrue(
                open.getElement().getAttributeNames().anyMatch(
                        name -> name.startsWith("data-flow-download-")),
                "The handler is registered on the button");
    }
}
