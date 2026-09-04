package com.example.acme;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.NativeTableRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterTableTest {

    @Test
    void namesItsColumnsWithTheCallersCountHeader() {
        MeterTable table = new MeterTable("Requests");

        List<String> headers = table.getHead().getRows().get(0).getChildren()
                .map(c -> c.getElement().getTextRecursively()).toList();
        assertEquals(List.of("Meter", "Tags", "Requests", "Value",
                "What it tells you"), headers);
    }

    @Test
    void rendersMetersAndTagsAsChipsAndValuesAsTimings() {
        MeterTable table = new MeterTable("Queries");
        table.setRows(List.of(new MeterTable.Row("vaadin.data.count.duration",
                "filtered=true", 3, "mean 1212 ms, max 1213 ms", "how long")));

        NativeTableRow row = table.getBody().getRows().get(0);
        assertEquals("vaadin.data.count.duration", cell(row, 0));
        assertTrue(row.getDataCell(0).orElseThrow().getElement().getChild(0)
                .getClassList().contains("metric"));
        assertEquals("filtered=true", cell(row, 1));
        assertEquals("3", cell(row, 2));
        assertEquals("mean 1212 ms, max 1213 ms", cell(row, 3));
        assertTrue(row.getDataCell(3).orElseThrow().getElement().getChild(0)
                .getClassList().contains("timing"));
        assertEquals("how long", cell(row, 4));
    }

    @Test
    void anUnmeasuredMeterReadsAsADashRatherThanZero() {
        MeterTable table = new MeterTable("Queries");
        table.setRows(List.of(
                new MeterTable.Row("vaadin.data.fetch.rows", "route=orders", 0,
                        "", "items returned")));

        assertEquals("—", cell(table.getBody().getRows().get(0), 3));
    }

    @Test
    void aGaugeHasNoRecordingCountToShow() {
        MeterTable table = new MeterTable("Samples");
        table.setRows(List.of(new MeterTable.Row("vaadin.sessions.active", "—",
                -1, "3", "signed-in users")));

        assertEquals("—", cell(table.getBody().getRows().get(0), 2));
    }

    @Test
    void setRowsReplacesRatherThanAppends() {
        MeterTable table = new MeterTable("Queries");
        table.setRows(List.of(new MeterTable.Row("a", "t", 1, "v", "r"),
                new MeterTable.Row("b", "t", 1, "v", "r")));
        table.setRows(List.of(new MeterTable.Row("c", "t", 1, "v", "r")));

        assertEquals(1, table.getBody().getRows().size());
        assertEquals("c", cell(table.getBody().getRows().get(0), 0));
    }

    private static String cell(NativeTableRow row, int index) {
        return row.getDataCell(index).orElseThrow().getElement()
                .getTextRecursively();
    }
}
