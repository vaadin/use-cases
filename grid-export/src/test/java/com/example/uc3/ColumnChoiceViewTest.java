package com.example.uc3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.example.data.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.ColumnReorderEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ColumnChoiceView.class)
class ColumnChoiceViewTest extends SpringBrowserlessTest {

    private static final List<String> ALL_HEADERS = List.of("Name",
            "Department", "Country", "Hired", "Salary");

    @Test
    void viewRendersEveryColumnByDefault() {
        ColumnChoiceView view = navigate(ColumnChoiceView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h1 -> "UC3 — The export follows the user's column choices"
                        .equals(h1.getText())));
        assertEquals(ColumnChoiceView.ROW_COUNT, test(view.grid).size());
        assertEquals(ALL_HEADERS, view.report().columnHeaders());
        assertEquals(ALL_HEADERS.size(), view.columnPicker.getValue().size());
    }

    @Test
    void untickingAColumnRemovesItFromTheExport() {
        ColumnChoiceView view = navigate(ColumnChoiceView.class);

        LinkedHashSet<Grid.Column<Employee>> kept = new LinkedHashSet<>(
                view.grid.getColumns());
        kept.removeIf(column -> "country".equals(column.getKey()));
        view.columnPicker.setValue(kept);

        assertEquals(List.of("Name", "Department", "Hired", "Salary"),
                view.report().columnHeaders());
        assertEquals(4, view.report().rows().get(0).size());
    }

    @Test
    void draggingAColumnReordersTheExport() {
        ColumnChoiceView view = navigate(ColumnChoiceView.class);

        List<Grid.Column<Employee>> reordered = new ArrayList<>(
                view.grid.getColumns());
        reordered.add(0, reordered.removeLast());
        ComponentUtil.fireEvent(view.grid,
                new ColumnReorderEvent<>(view.grid, true, reordered));

        assertEquals(
                List.of("Salary", "Name", "Department", "Country", "Hired"),
                view.report().columnHeaders(),
                "the export should follow the order the user dragged");
        assertEquals(ALL_HEADERS,
                view.grid.getColumns().stream()
                        .map(column -> column.getHeaderText()).toList(),
                "the grid itself still reports the declaration order — this is "
                        + "the gap the view works around");
    }

    @Test
    void hidingAndReorderingCombine() {
        ColumnChoiceView view = navigate(ColumnChoiceView.class);

        List<Grid.Column<Employee>> reordered = new ArrayList<>(
                view.grid.getColumns());
        reordered.add(0, reordered.removeLast());
        ComponentUtil.fireEvent(view.grid,
                new ColumnReorderEvent<>(view.grid, true, reordered));

        LinkedHashSet<Grid.Column<Employee>> kept = new LinkedHashSet<>(
                view.grid.getColumns());
        kept.removeIf(column -> "hired".equals(column.getKey()));
        view.columnPicker.setValue(kept);

        assertEquals(List.of("Salary", "Name", "Department", "Country"),
                view.report().columnHeaders());
    }
}
