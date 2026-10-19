package com.example.uc1;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.data.provider.SortDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CsvOfCurrentViewView.class)
class CsvOfCurrentViewViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersGridAndCsvPreview() {
        CsvOfCurrentViewView view = navigate(CsvOfCurrentViewView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h1 -> "UC1 — CSV of the current view".equals(h1.getText())));
        assertEquals(CsvOfCurrentViewView.ROW_COUNT, test(view.grid).size());
        assertEquals("Name", test(view.grid).getHeaderCell(0));

        // The preview holds the very bytes the download link would produce.
        String preview = findInView(Pre.class).first().getText();
        assertEquals(view.csv(), preview);
        assertEquals(
                List.of("Employees", "Name,Department,Country,Hired,Salary"),
                preview.lines().limit(2).toList());
        assertEquals(CsvOfCurrentViewView.ROW_COUNT,
                view.report().rows().size());
    }

    @Test
    void exportFollowsTheActiveFilter() {
        CsvOfCurrentViewView view = navigate(CsvOfCurrentViewView.class);

        view.filter.setValue("Engineering");

        List<List<String>> rows = view.report().rows();
        assertEquals(test(view.grid).size(), rows.size(),
                "the export should hold exactly the rows the grid shows");
        assertFalse(rows.isEmpty(), "the sample data has engineers");
        assertTrue(rows.stream()
                .allMatch(row -> "Engineering".equals(row.get(1))));
        assertEquals(view.csv(), findInView(Pre.class).first().getText(),
                "the preview should follow the filter");
    }

    @Test
    void exportFollowsTheActiveSorting() {
        CsvOfCurrentViewView view = navigate(CsvOfCurrentViewView.class);

        test(view.grid).sortByColumn("name", SortDirection.ASCENDING);
        List<String> ascending = names(view);
        assertEquals(ascending.stream().sorted().toList(), ascending,
                "sorting the grid should reorder the export");

        test(view.grid).sortByColumn("name", SortDirection.DESCENDING);
        assertEquals(
                ascending.stream().sorted(Comparator.reverseOrder()).toList(),
                names(view));
    }

    @Test
    void filteringEverythingAwayExportsTheEmptyStateText() {
        CsvOfCurrentViewView view = navigate(CsvOfCurrentViewView.class);

        view.filter.setValue("no-such-employee");

        assertEquals(0, view.report().rows().size());
        assertEquals("No employees match the current filter.",
                view.report().emptyStateText());
        assertTrue(
                view.csv().contains("No employees match the current filter."));
    }

    /** The exported Name column, which is also the grid's first column. */
    private static List<String> names(CsvOfCurrentViewView view) {
        return view.report().rows().stream().map(row -> row.get(0)).toList();
    }
}
