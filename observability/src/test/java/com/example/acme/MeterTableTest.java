package com.example.acme;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.TableRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterTableTest {

    @Test
    void namesItsColumnsWithTheCallersCountHeader() {
        MeterTable table = new MeterTable("Requests");

        List<String> headers = table.getHeaderRows().get(0).getHeaderCells()
                .stream().map(c -> c.getElement().getTextRecursively())
                .toList();
        assertEquals(List.of("Meter", "Tags", "Requests", "Value",
                "What it tells you"), headers);
    }

    @Test
    void rendersMetersAndTagsAsChipsAndValuesAsTimings() {
        MeterTable table = new MeterTable("Queries");
        table.setRows(List.of(new MeterTable.Row("vaadin.data.count.duration",
                "filtered=true", 3, "mean 1212 ms, max 1213 ms", "how long")));

        TableRow row = table.getBodyRows().get(0);
        assertEquals("vaadin.data.count.duration", cell(row, 0));
        assertTrue(row.getDataCells().get(0).getElement().getChild(0)
                .getClassList().contains("metric"));
        assertEquals("filtered=true", cell(row, 1));
        assertEquals("3", cell(row, 2));
        assertEquals("mean 1212 ms, max 1213 ms", cell(row, 3));
        assertTrue(row.getDataCells().get(3).getElement().getChild(0)
                .getClassList().contains("timing"));
        assertEquals("how long", cell(row, 4));
    }

    @Test
    void anUnmeasuredMeterReadsAsADashRatherThanZero() {
        MeterTable table = new MeterTable("Queries");
        table.setRows(List.of(
                new MeterTable.Row("vaadin.data.fetch.rows", "route=orders", 0,
                        "", "items returned")));

        assertEquals("—", cell(table.getBodyRows().get(0), 3));
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

        assertEquals(1, table.getBodyRows().size());
        assertEquals("c", cell(table.getBodyRows().get(0), 0));
    }

    private static String cell(TableRow row, int index) {
        return row.getDataCells().get(index).getElement()
                .getTextRecursively();
    }
}
