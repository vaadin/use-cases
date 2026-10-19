package com.example.uc9;

import java.util.List;

import com.example.documents.SampleDocuments.PurchaseOrder;
import com.example.pdfviewer.PdfViewer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ApproveInToolbarView.class)
class ApproveInToolbarViewTest extends SpringBrowserlessTest {

    @Test
    void theDecisionButtonsAreInTheViewersToolbarWithTooltips() {
        navigate(ApproveInToolbarView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();

        List<Button> buttons = find(Button.class).from(viewer).all();

        assertEquals(List.of("Approve", "Reject"), buttons.stream()
                .map(button -> button.getAriaLabel().get()).toList());
        for (Button button : buttons) {
            assertEquals("toolbar-actions",
                    button.getElement().getAttribute("slot"));
            assertFalse(button.getTooltip().getText().isBlank());
        }
    }

    @Test
    void aDecisionIsShownInTheListAndOpensTheNextWaitingOrder() {
        navigate(ApproveInToolbarView.class);
        assertTrue(src().endsWith("purchase-order-PO-3301.pdf"), src());

        test(button("Approve")).click();
        assertEquals("Approved", test(orders()).getCellText(0, 3));
        assertTrue(src().endsWith("purchase-order-PO-3302.pdf"), src());

        test(button("Reject")).click();
        assertEquals("Rejected", test(orders()).getCellText(1, 3));
        assertTrue(src().endsWith("purchase-order-PO-3303.pdf"), src());
        assertEquals("Waiting", test(orders()).getCellText(2, 3));
    }

    @Test
    void aDecidedOrderCanNotBeDecidedAgain() {
        navigate(ApproveInToolbarView.class);
        test(button("Approve")).click();

        test(orders()).select(0);

        assertFalse(button("Approve").isEnabled());
        assertFalse(button("Reject").isEnabled());
    }

    private Button button(String label) {
        return find(Button.class).from(findInView(PdfViewer.class).single())
                .all().stream().filter(button -> button.getAriaLabel()
                        .orElse("").equals(label))
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Grid<PurchaseOrder> orders() {
        return findInView(Grid.class).single();
    }

    private String src() {
        return findInView(PdfViewer.class).single().getElement()
                .getAttribute("src");
    }
}
