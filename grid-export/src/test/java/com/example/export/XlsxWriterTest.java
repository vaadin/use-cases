package com.example.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import com.example.export.ExportedGrid.Alignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The workbook writing rules, away from any Grid.
 */
class XlsxWriterTest {

    @Test
    void aTitleThatIsNotAValidSheetNameStillProducesAWorkbook()
            throws IOException {
        String title = "Expenses by department, 2026/01 – 2026/03 [draft]";
        ExportedGrid report = new ExportedGrid(title, List.of(),
                List.of("Name"), List.of(Alignment.START),
                List.of(List.of("Ada")), List.of(), "No rows.");

        byte[] bytes = XlsxWriter.toBytes(report);

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(bytes))) {
            String sheetName = workbook.getSheetName(0);
            assertEquals("Expenses by department, 2026 01", sheetName);
        }
        assertEquals(title, XlsxWriter.readBack(bytes).get(0).get(0));
    }
}
