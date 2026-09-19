package com.example.export;

import java.util.List;

import com.example.export.ExportedGrid.Alignment;
import com.example.export.ExportedGrid.HeaderCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The CSV writing rules, away from any Grid: quoting, the flattening of
 * spanning header cells, and the empty report.
 */
class CsvWriterTest {

    @Test
    void cellsContainingSeparatorsQuotesOrNewlinesAreQuoted() {
        assertEquals("plain", CsvWriter.toLine(List.of("plain")));
        assertEquals("\"45,000.00 EUR\"",
                CsvWriter.toLine(List.of("45,000.00 EUR")));
        assertEquals("\"say \"\"hi\"\"\"",
                CsvWriter.toLine(List.of("say \"hi\"")));
        assertEquals("\"two\nlines\"", CsvWriter.toLine(List.of("two\nlines")));
        assertEquals("a,\"b,c\",d", CsvWriter.toLine(List.of("a", "b,c", "d")));
    }

    @Test
    void aSpanningHeaderCellFillsTheColumnsItCovers() {
        ExportedGrid report = new ExportedGrid("Report",
                List.of(List.of(new HeaderCell("Employee", 2),
                        new HeaderCell("Compensation", 2))),
                List.of("Name", "Department", "Salary", "Bonus"),
                List.of(Alignment.START, Alignment.START, Alignment.END,
                        Alignment.END),
                List.of(List.of("Ada", "Engineering", "1", "2")), List.of(),
                "No rows.");

        assertEquals(
                List.of("Report", "Employee,,Compensation,",
                        "Ada,Engineering,1,2"),
                CsvWriter.toCsv(report).lines().toList());
    }

    @Test
    void anEmptyReportCarriesTheEmptyStateText() {
        ExportedGrid report = new ExportedGrid("", List.of(), List.of("Name"),
                List.of(Alignment.START), List.of(), List.of(),
                "Nothing to report.");

        assertEquals(List.of("Nothing to report."),
                CsvWriter.toCsv(report).lines().toList());
    }
}
