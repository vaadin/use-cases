package com.example.acme;

import java.util.List;

import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;

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
public class MeterTable extends Table {

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

    /**
     * @param countHeader
     *            the header of the count column, naming what is counted:
     *            "Queries", "Requests", …
     */
    public MeterTable(String countHeader) {
        addClassName("meter-table");
        setWidthFull();
        addHeaderRow("Meter", "Tags", countHeader, "Value",
                "What it tells you");
    }

    /** Replaces the rows. */
    public void setRows(List<Row> newRows) {
        List.copyOf(getBody().getRows()).forEach(TableRow::removeFromParent);
        newRows.forEach(this::render);
    }

    private void render(Row row) {
        TableRow tr = getBody().addRow();
        tr.addDataCell(Telemetry.chip(row.meter()));
        tr.addDataCell(Telemetry.chip(row.tags()));
        tr.addDataCell(row.count() < 0 ? "—" : Long.toString(row.count()));
        if (row.value().isEmpty()) {
            tr.addDataCell("—");
        } else {
            tr.addDataCell(Telemetry.timing(row.value()));
        }
        tr.addDataCell(row.reads());
    }
}
