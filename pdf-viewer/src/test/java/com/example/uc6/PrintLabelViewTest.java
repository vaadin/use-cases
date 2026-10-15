package com.example.uc6;

import com.example.documents.SampleDocuments.Order;
import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrintLabelView.class)
class PrintLabelViewTest extends SpringBrowserlessTest {

    @Test
    void aLabelThatIsNotShownYetIsPrintedOnceItHasLoaded() {
        navigate(PrintLabelView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        PdfViewerTester tester = new PdfViewerTester(viewer);
        tester.load(1, "Acme Parcel");
        assertFalse(tester.printRequested(),
                "The first label is shown on arrival, not printed");

        test(printButton(1)).click();
        assertTrue(viewer.getElement().getAttribute("src")
                .endsWith("label-A-20932.pdf"));
        assertFalse(tester.printRequested(),
                "print() does nothing before the document has loaded");

        tester.load(1, "Acme Parcel");
        assertTrue(tester.printRequested());
        assertTrue(log().contains("order A-20932"), log());
    }

    @Test
    void aLabelThatIsShownAlreadyIsPrintedAtOnce() {
        navigate(PrintLabelView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        PdfViewerTester tester = new PdfViewerTester(viewer);
        test(printButton(0)).click();
        tester.load(1, "");
        tester.printRequested();
        String src = viewer.getElement().getAttribute("src");

        test(printButton(0)).click();

        assertTrue(tester.printRequested());
        assertEquals(src, viewer.getElement().getAttribute("src"),
                "The label is not loaded again");
    }

    private Button printButton(int row) {
        @SuppressWarnings("unchecked")
        Grid<Order> orders = findInView(Grid.class).single();
        return (Button) test(orders).getCellComponent(row, 2);
    }

    private String log() {
        return findInView(Paragraph.class).withClassName("uc6-log").single()
                .getText();
    }
}
