package com.example.uc2;

import java.util.List;

import com.example.data.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.data.provider.SortDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ExportSelectionView.class)
class ExportSelectionViewTest extends SpringBrowserlessTest {

    @Test
    void nothingSelectedExportsNoRows() {
        ExportSelectionView view = navigate(ExportSelectionView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h1 -> "UC2 — Export only the selected rows"
                        .equals(h1.getText())));
        assertEquals(ExportSelectionView.ROW_COUNT, test(view.grid).size());
        assertEquals(List.of(), view.report().rows());
        assertTrue(view.csv().contains("No rows selected."),
                "an empty report should carry the grid's empty-state text");
    }

    @Test
    void onlySelectedRowsAreExported() {
        ExportSelectionView view = navigate(ExportSelectionView.class);

        test(view.grid).select(5);
        test(view.grid).select(1);

        List<List<String>> rows = view.report().rows();
        assertEquals(2, rows.size());
        assertEquals(view.csv(), findInView(Pre.class).first().getText(),
                "the preview should follow the selection");
    }

    @Test
    void selectionIsExportedInTheGridsRowOrderNotTheSelectionOrder() {
        ExportSelectionView view = navigate(ExportSelectionView.class);

        // Picked bottom-up on purpose: getSelectedItems() is an unordered Set,
        // so a naive export could emit these in either order.
        test(view.grid).select(5);
        test(view.grid).select(1);

        Employee first = test(view.grid).getRow(1);
        Employee second = test(view.grid).getRow(5);
        assertEquals(List.of(first.name(), second.name()), exportedNames(view));

        // Re-sorting the grid without touching the selection must reorder the
        // report too.
        test(view.grid).sortByColumn("name", SortDirection.DESCENDING);
        List<String> descending = exportedNames(view);
        assertEquals(2, descending.size());
        assertEquals(List.of(first.name(), second.name()).stream()
                .sorted((left, right) -> right.compareTo(left)).toList(),
                descending);
    }

    private static List<String> exportedNames(ExportSelectionView view) {
        return view.report().rows().stream().map(row -> row.get(0)).toList();
    }
}
