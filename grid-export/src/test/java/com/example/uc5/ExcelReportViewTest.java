package com.example.uc5;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import com.example.export.ExportedGrid.HeaderCell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ExcelReportView.class)
class ExcelReportViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersGroupedHeaderAndFooterTotals() {
        ExcelReportView view = navigate(ExcelReportView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h1 -> h1.getText().startsWith("UC5 — A spreadsheet report")));
        assertEquals(ExcelReportView.ROW_COUNT, test(view.grid).size());
        assertEquals("Salary", test(view.grid).getHeaderCell(2));
        assertEquals(2, view.grid.getHeaderRows().size());
        assertEquals(1, view.grid.getFooterRows().size());
    }

    @Test
    void reportCarriesBothHeaderRowsWithTheGroupSpans() {
        ExcelReportView view = navigate(ExcelReportView.class);

        List<List<HeaderCell>> headerRows = view.report().headerRows();
        assertEquals(2, headerRows.size());
        assertEquals(
                List.of(new HeaderCell("Employee", 2),
                        new HeaderCell("Compensation", 2)),
                headerRows.get(0),
                "the grouped row comes first, each group spanning 2 columns");
        assertEquals(List.of(new HeaderCell("Name", 1),
                new HeaderCell("Department", 1), new HeaderCell("Salary", 1),
                new HeaderCell("Bonus", 1)), headerRows.get(1));
    }

    @Test
    void workbookMergesTheGroupedHeaderCells() throws IOException {
        ExcelReportView view = navigate(ExcelReportView.class);

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(view.workbook()))) {
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals(ExcelReportView.TITLE, sheet.getSheetName());
            assertEquals(ExcelReportView.TITLE,
                    sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Employee",
                    sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Compensation",
                    sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("Bonus",
                    sheet.getRow(2).getCell(3).getStringCellValue());

            List<String> merged = sheet.getMergedRegions().stream()
                    .map(CellRangeAddress::formatAsString).toList();
            assertTrue(merged.contains("A2:B2"),
                    "the Employee group should be a merged cell: " + merged);
            assertTrue(merged.contains("C2:D2"),
                    "the Compensation group should be a merged cell: "
                            + merged);
            assertTrue(merged.contains("A1:D1"),
                    "the title should span the report: " + merged);
        }
    }

    @Test
    void workbookEndsWithTheFooterTotals() throws IOException {
        ExcelReportView view = navigate(ExcelReportView.class);

        List<String> footer = view.report().footerRows().get(0);
        assertEquals("Total (" + ExcelReportView.ROW_COUNT + ")",
                footer.get(0));

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(view.workbook()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // title + 2 header rows + data + footer
            int lastRow = sheet.getLastRowNum();
            assertEquals(3 + ExcelReportView.ROW_COUNT, lastRow);
            assertEquals(footer.get(0),
                    sheet.getRow(lastRow).getCell(0).getStringCellValue());
            assertEquals(footer.get(2),
                    sheet.getRow(lastRow).getCell(2).getStringCellValue());
        }
    }
}
