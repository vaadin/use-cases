package com.example.uc6;

import com.example.documents.SampleDocuments.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.pdfviewer.PdfViewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrintLabelView.class)
class PrintLabelViewTest extends SpringBrowserlessTest {

    @Test
    void pickingAnOrderShowsItsLabel() {
        navigate(PrintLabelView.class);
        assertTrue(src().endsWith("label-A-20931.pdf"), src());

        test(orders()).select(1);

        assertTrue(src().endsWith("label-A-20932.pdf"), src());
    }

    @Test
    void printLabelOpensTheLabelInlineInANewTab() {
        navigate(PrintLabelView.class);

        Anchor link = (Anchor) test(orders()).getCellComponent(1, 2);

        assertEquals("Print label", link.getText());
        assertEquals(AnchorTarget.BLANK.getValue(),
                link.getTarget().orElseThrow());
        assertTrue(link.getHref().endsWith("label-A-20932.pdf"),
                link.getHref());
    }

    @SuppressWarnings("unchecked")
    private Grid<Order> orders() {
        return findInView(Grid.class).single();
    }

    private String src() {
        return findInView(PdfViewer.class).single().getElement()
                .getAttribute("src");
    }
}
