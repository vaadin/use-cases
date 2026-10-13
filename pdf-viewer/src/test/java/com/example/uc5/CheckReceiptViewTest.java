package com.example.uc5;

import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CheckReceiptView.class)
class CheckReceiptViewTest extends SpringBrowserlessTest {

    @Test
    void theReceiptIsFittedNextToTheClaim() {
        navigate(CheckReceiptView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();

        assertEquals(PdfViewer.Zoom.PAGE_FIT, viewer.getZoom());
        assertTrue(viewer.getElement().getProperty("toolbarCollapsed", false));
        assertEquals("32.40 EUR", field("Claimed amount"));
        assertTrue(viewer.getElement().getAttribute("src")
                .endsWith("receipt-EXP-118.pdf"));
    }

    @Test
    void decisionsStayWithTheirClaim() {
        navigate(CheckReceiptView.class);
        test(findInView(Button.class).withText("Send back").single()).click();
        assertEquals("Sent back to Maria Lind", decision());

        var second = SampleDocuments.EXPENSE_CLAIMS.get(1);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        String firstSrc = viewer.getElement().getAttribute("src");
        test(findInView(Select.class).single())
                .selectItem(second.id() + " — " + second.employee());
        assertEquals("Waiting for your decision", decision());
        assertEquals("City Taxi", field("Merchant"));
        assertNotEquals(firstSrc, viewer.getElement().getAttribute("src"));

        var first = SampleDocuments.EXPENSE_CLAIMS.getFirst();
        test(findInView(Select.class).single())
                .selectItem(first.id() + " — " + first.employee());
        assertEquals("Sent back to Maria Lind", decision());
    }

    private String field(String label) {
        return findInView(TextField.class).withCaption(label).single()
                .getValue();
    }

    private String decision() {
        return findInView(Span.class).withClassName("uc5-decision").single()
                .getText();
    }
}
