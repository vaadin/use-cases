package com.example.uc2;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.AiTestConfiguration;
import com.example.FakeLlm;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = InvoiceReaderView.class)
class InvoiceReaderViewTest extends SpringBrowserlessTest {

    private static final String OFFICE_SUPPLIES_VALUES = """
            {"values": {"vendor": "Northwind Office Supplies GmbH",
              "invoiceNumber": "NW-2026-0412", "invoiceDate": "2026-09-03",
              "dueDate": "2026-10-03", "currency": "EUR",
              "netAmount": 2220.00, "vatAmount": 421.80,
              "totalAmount": 2641.80, "iban": "DE89370400440532013000"}}""";

    @Autowired
    private FakeLlm llm;

    @BeforeEach
    void resetLlm() {
        llm.reset();
    }

    @Test
    void sampleInvoiceIsSentAsPdfAndTheModelFillsTheForm() throws IOException {
        llm.respond(request -> {
            FakeLlm.callTool(request, "fill_form", OFFICE_SUPPLIES_VALUES);
            return "I filled in the invoice from Northwind.";
        });
        navigate(InvoiceReaderView.class);

        test(find(Button.class).withText("Office supplies invoice").single())
                .click();
        runPendingSignalsTasks();

        List<AIAttachment> attachments = llm.requests().getFirst()
                .attachments();
        assertEquals(1, attachments.size());
        assertEquals("application/pdf", attachments.getFirst().mimeType());
        assertTrue(
                textOf(attachments.getFirst().data()).contains("NW-2026-0412"),
                "the PDF carries the invoice");

        assertEquals("Northwind Office Supplies GmbH", field("vendor"));
        assertEquals(LocalDate.of(2026, 10, 3),
                find(DatePicker.class).withId("dueDate").single().getValue());
        assertEquals(0,
                new BigDecimal("2641.80").compareTo(find(BigDecimalField.class)
                        .withId("totalAmount").single().getValue()));
        assertTrue(status().contains("I filled in the invoice"), status());
    }

    @Test
    void approvingMovesTheInvoiceToTheApprovedListAndClearsTheForm() {
        llm.respond(request -> {
            FakeLlm.callTool(request, "fill_form", OFFICE_SUPPLIES_VALUES);
            return "Done.";
        });
        navigate(InvoiceReaderView.class);
        test(find(Button.class).withText("Office supplies invoice").single())
                .click();
        runPendingSignalsTasks();

        test(find(Button.class).withText("Approve for payment").single())
                .click();

        @SuppressWarnings("unchecked")
        Grid<Invoice> approved = find(Grid.class).single();
        assertEquals(1, test(approved).size());
        assertEquals("NW-2026-0412",
                test(approved).getRow(0).getInvoiceNumber());
        assertEquals("", field("vendor"));
    }

    @Test
    void amountsThatDoNotAddUpAreRejectedToTheModelAndBlockApproval() {
        String[] toolResult = new String[1];
        llm.respond(request -> {
            toolResult[0] = FakeLlm.callTool(request, "fill_form", """
                    {"values": {"vendor": "Fabrikam Consulting Ltd",
                      "invoiceNumber": "7781", "invoiceDate": "2026-09-15",
                      "dueDate": "2026-09-29", "currency": "GBP",
                      "netAmount": 3840.00, "vatAmount": 768.00,
                      "totalAmount": 4680.00,
                      "iban": "GB29NWBK60161331926819"}}""");
            return "The amounts disagree.";
        });
        navigate(InvoiceReaderView.class);
        test(find(Button.class).withText(SampleInvoices.CONSULTING.label())
                .single()).click();
        runPendingSignalsTasks();

        assertTrue(
                toolResult[0].contains(
                        "Net amount plus VAT does not equal the total"),
                "the model learns about the failed cross-field rule: "
                        + toolResult[0]);

        test(find(Button.class).withText("Approve for payment").single())
                .click();

        assertEquals("Net amount plus VAT does not equal the total", status());
        assertEquals(0, test(find(Grid.class).single()).size());
    }

    private String field(String id) {
        return find(TextField.class).withId(id).single().getValue();
    }

    private String status() {
        return find(Paragraph.class).withClassName("uc2-status").single()
                .getText();
    }

    private static String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
