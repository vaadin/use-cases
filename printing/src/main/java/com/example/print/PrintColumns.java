package com.example.print;

import java.util.List;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableDataCell;
import com.vaadin.flow.component.html.TableHeaderCell;
import com.vaadin.flow.component.html.TableRow;

/**
 * Renders a list of {@link PrintColumn}s either as Grid columns or as a plain
 * HTML {@link Table}.
 * <p>
 * The table is what reaches the paper: it is light DOM, so print CSS can reach
 * it, its {@code thead} repeats on every page, and the browser can break it
 * between rows. A Grid can do none of those things.
 */
public final class PrintColumns {

    private PrintColumns() {
    }

    /**
     * Adds the columns to a Grid.
     *
     * @param <T>
     *            the row type
     * @param grid
     *            the grid to add the columns to
     * @param columns
     *            the columns to add
     */
    public static <T> void addTo(Grid<T> grid, List<PrintColumn<T>> columns) {
        columns.forEach(column -> {
            Grid.Column<T> gridColumn = grid
                    .addColumn(item -> column.value().apply(item))
                    .setHeader(column.header()).setAutoWidth(true);
            if (column.numeric()) {
                gridColumn.setTextAlign(ColumnTextAlign.END);
            }
        });
    }

    /**
     * Builds the printable table for the same columns and the given rows.
     *
     * @param <T>
     *            the row type
     * @param columns
     *            the columns to render
     * @param items
     *            the rows to render, in the order they should be printed
     * @return a table with one header row and one body row per item
     */
    public static <T> Table asTable(List<PrintColumn<T>> columns,
            List<T> items) {
        Table table = new Table();
        table.addClassName("doc-table");
        table.setWidthFull();

        TableRow header = table.addHeaderRow();
        columns.forEach(column -> {
            TableHeaderCell cell = header.addColumnHeaderCell(column.header());
            if (column.numeric()) {
                cell.addClassName("numeric");
            }
        });

        items.forEach(item -> {
            TableRow row = table.addRow();
            columns.forEach(column -> {
                TableDataCell cell = row
                        .addDataCell(column.value().apply(item));
                if (column.numeric()) {
                    cell.addClassName("numeric");
                }
            });
        });
        return table;
    }
}
