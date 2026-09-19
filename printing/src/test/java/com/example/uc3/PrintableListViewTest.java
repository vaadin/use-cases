package com.example.uc3;

import java.util.List;

import com.example.PrintTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrintableListView.class)
class PrintableListViewTest extends SpringBrowserlessTest {

    @Test
    void theTableOnPaperHasEveryRowTheGridHas() {
        navigate(PrintableListView.class);

        Grid<?> grid = findInView(Grid.class).single();
        Table table = findInView(Table.class).id("printable-table");

        assertEquals(120, test(grid).size(),
                "The Grid renders a few rows but holds all of them");
        assertEquals(120, table.getBodyRows().size(),
                "All of them have to be in the printable table, because the "
                        + "printer only sees rendered DOM");

        assertEquals(
                PrintableListView.COLUMNS.stream()
                        .map(column -> column.header()).toList(),
                table.getHeaderRows().getFirst().getCells().stream()
                        .map(cell -> cell.getElement().getTextRecursively())
                        .toList(),
                "Both renderings are built from the same column list");
        assertEquals(test(grid).getCellText(0, 0), cellText(table, 0, 0));
    }

    @Test
    void changingTheSortOrderRebuildsBothRenderings() {
        navigate(PrintableListView.class);

        Grid<?> grid = findInView(Grid.class).single();
        assertEquals("ORD-1001", cellText(findTable(), 0, 0));

        test(findInView(Select.class).id("sort-select"))
                .selectItem("Total, largest first");

        Table table = findTable();
        assertEquals(test(grid).getCellText(0, 0), cellText(table, 0, 0),
                "The printed table has to follow the Grid's order — the "
                        + "application is what keeps them in sync");
        List<String> totals = table.getBodyRows().stream()
                .map(row -> cellText(row, 4)).toList();
        assertEquals(totals.stream().sorted(this::byAmountDescending).toList(),
                totals, "Largest first means largest first on paper too");
    }

    @Test
    void printButtonOpensThePrintDialog() {
        navigate(PrintableListView.class);

        test(findInView(Button.class).id("print-button")).click();

        assertTrue(PrintTestSupport.printRequested());
    }

    private Table findTable() {
        return findInView(Table.class).id("printable-table");
    }

    private static String cellText(Table table, int row, int column) {
        return cellText(table.getBodyRows().get(row), column);
    }

    private static String cellText(TableRow row, int column) {
        return row.getCells().get(column).getElement().getTextRecursively();
    }

    private int byAmountDescending(String left, String right) {
        return Double.compare(amount(right), amount(left));
    }

    private static double amount(String money) {
        return Double
                .parseDouble(money.replace(",", "").replace(" €", "").trim());
    }
}
