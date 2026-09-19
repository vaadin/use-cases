package com.example.uc8;

import java.util.List;

import com.example.data.Employee;
import com.example.export.ExportedGrid.Alignment;
import com.example.export.PdfWriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MultiPagePdfView.class)
class MultiPagePdfViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersTheGroupedGridAndTheGeneratedPdf() {
        MultiPagePdfView view = navigate(MultiPagePdfView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h1 -> h1.getText().startsWith("UC8 — A multi-page PDF")));
        assertEquals(MultiPagePdfView.ROW_COUNT, test(view.grid).size());
        assertEquals(2, view.grid.getHeaderRows().size());
        assertEquals(
                List.of(Alignment.START, Alignment.START, Alignment.END,
                        Alignment.END),
                view.report().alignments(),
                "the money columns are right-aligned in the grid, so the "
                        + "report knows to right-align them too");

        int pages = PdfWriter.readBackPages(view.pdf()).size();
        assertEquals(
                MultiPagePdfView.ROW_COUNT + " rows over " + pages
                        + " pages, with both header rows on each of them.",
                view.stats.getText());
    }

    @Test
    void theReportSpansSeveralPages() {
        MultiPagePdfView view = navigate(MultiPagePdfView.class);

        List<String> pages = PdfWriter.readBackPages(view.pdf());

        assertTrue(pages.size() > 1, MultiPagePdfView.ROW_COUNT
                + " rows should not fit on one " + "page, got " + pages.size());
        assertTrue(pages.get(0).contains(MultiPagePdfView.TITLE),
                "the title belongs on the first page");
        assertTrue(pages.get(0).contains("Page 1 of " + pages.size()),
                "the page number should count the pages that were produced");
    }

    @Test
    void bothHeaderRowsAreRepeatedOnEveryPage() {
        MultiPagePdfView view = navigate(MultiPagePdfView.class);

        List<String> pages = PdfWriter.readBackPages(view.pdf());

        for (int page = 0; page < pages.size(); page++) {
            String text = pages.get(page);
            assertTrue(
                    text.contains("Employee") && text.contains("Compensation"),
                    "the grouped header row is missing from page "
                            + (page + 1));
            for (String header : view.report().columnHeaders()) {
                assertTrue(text.contains(header), "column header '" + header
                        + "' is missing from page " + (page + 1));
            }
        }
    }

    @Test
    void theTotalsAppearOnceOnTheLastPage() {
        MultiPagePdfView view = navigate(MultiPagePdfView.class);

        List<String> pages = PdfWriter.readBackPages(view.pdf());
        String total = "Total (" + MultiPagePdfView.ROW_COUNT + ")";

        assertTrue(pages.getLast().contains(total),
                "the totals belong after the last row");
        assertEquals(1,
                pages.stream().filter(page -> page.contains(total)).count(),
                "the totals must not repeat like the headers do");
    }

    @Test
    void everyRowReachesThePdf() {
        MultiPagePdfView view = navigate(MultiPagePdfView.class);

        String all = String.join("\n", PdfWriter.readBackPages(view.pdf()));
        List<List<String>> rows = view.report().rows();
        assertEquals(MultiPagePdfView.ROW_COUNT, rows.size());

        // Spot-check the ends and the middle: the whole file is one text dump,
        // so a row that fell into a page break would be missing from it.
        for (int row : new int[] { 0, rows.size() / 2, rows.size() - 1 }) {
            Employee employee = test(view.grid).getRow(row);
            assertTrue(all.contains(employee.name()), "row " + row + " ("
                    + employee.name() + ") is missing from the PDF");
        }
        assertFalse(all.contains("?"),
                "the demo data should not need character substitution");
    }
}
