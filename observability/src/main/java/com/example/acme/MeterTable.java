package com.example.acme;

import java.util.List;

import com.vaadin.flow.component.html.NativeTable;
import com.vaadin.flow.component.html.NativeTableBody;
import com.vaadin.flow.component.html.NativeTableCell;
import com.vaadin.flow.component.html.NativeTableHeaderCell;
import com.vaadin.flow.component.html.NativeTableRow;

/**
 * The raw-meters table at the end of an investigation: one row per meter the
 * view reads, with the tags it reads it by, how many recordings it has, the
 * value, and what that value tells the reader.
 * <p>
 * Deliberately a plain HTML table rather than a {@code Grid}: the kit
 * instruments every {@code DataCommunicator}, in-memory ones included, so a
 * {@code Grid} showing the meters would record data queries on the very route
 * whose meters it displays.
 */
public class MeterTable extends NativeTable {

    /**
     * One row.
     *
     * @param meter
     *            the meter name
     * @param tags
     *            the tags the meter is read by, e.g. {@code filtered=true}
     * @param count
     *            how many recordings the reading aggregates, or {@code -1}
     *            when that has no meaning (a gauge)
     * @param value
     *            the formatted value, empty when there are no recordings
     * @param reads
     *            what the value tells the reader
     */
    public record Row(String meter, String tags, long count, String value,
            String reads) {
    }

    private final NativeTableBody rows = getBody();

    /**
     * @param countHeader
     *            the header of the count column, naming what is counted:
     *            "Queries", "Requests", …
     */
    public MeterTable(String countHeader) {
        addClassName("meter-table");
        setWidthFull();
        NativeTableRow header = getHead().addRow();
        for (String title : List.of("Meter", "Tags", countHeader, "Value",
                "What it tells you")) {
            header.add(new NativeTableHeaderCell(title));
        }
    }

    /** Replaces the rows. */
    public void setRows(List<Row> newRows) {
        rows.removeAllRows();
        newRows.forEach(row -> rows.add(render(row)));
    }

    private static NativeTableRow render(Row row) {
        NativeTableCell meter = new NativeTableCell();
        meter.add(Telemetry.chip(row.meter()));
        NativeTableCell tags = new NativeTableCell();
        tags.add(Telemetry.chip(row.tags()));
        NativeTableCell value = new NativeTableCell();
        if (row.value().isEmpty()) {
            value.setText("—");
        } else {
            value.add(Telemetry.timing(row.value()));
        }
        return new NativeTableRow(meter, tags,
                new NativeTableCell(
                        row.count() < 0 ? "—" : Long.toString(row.count())),
                value, new NativeTableCell(row.reads()));
    }
}
