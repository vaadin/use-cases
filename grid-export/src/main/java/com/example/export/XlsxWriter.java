package com.example.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.export.ExportedGrid.HeaderCell;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Writes an {@link ExportedGrid} as a styled {@code .xlsx} workbook — the
 * "advanced" format from use case [2] of
 * <a href="https://github.com/vaadin/platform/issues/7196">vaadin/platform
 * #7196</a>: a title, the grid's header rows with their joins kept as merged
 * cells, the data, and the footer aggregates.
 */
public final class XlsxWriter {

    /** MIME type for an OOXML spreadsheet. */
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final int MAX_AUTOSIZED_WIDTH = 60 * 256;

    private XlsxWriter() {
    }

    /** Renders the report as workbook bytes. */
    public static byte[] toBytes(ExportedGrid exported) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            write(workbook, exported);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A download of the workbook. The supplier runs inside the download
     * request, so the file reflects the grid as it is when the user clicks.
     */
    public static DownloadHandler download(String fileName,
            Supplier<ExportedGrid> report) {
        return event -> {
            byte[] bytes = toBytes(report.get());
            event.setFileName(fileName);
            event.setContentType(CONTENT_TYPE);
            event.setContentLength(bytes.length);
            event.getOutputStream().write(bytes);
        };
    }

    /**
     * Reads a workbook back as rows of text, so a view can show what it just
     * produced without the user having to open a spreadsheet application.
     */
    public static List<List<String>> readBack(byte[] bytes) {
        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<List<String>> rows = new ArrayList<>();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    rows.add(List.of());
                    continue;
                }
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    cells.add(cell == null ? "" : cell.toString());
                }
                rows.add(List.copyOf(cells));
            }
            return List.copyOf(rows);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Workbook workbook, ExportedGrid exported) {
        Sheet sheet = workbook.createSheet(
                exported.title().isEmpty() ? "Export" : exported.title());
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle footerStyle = footerStyle(workbook);

        int columnCount = Math.max(1, exported.columnCount());
        int rowIndex = 0;

        if (!exported.title().isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            cell(row, 0, exported.title(), titleStyle);
            if (columnCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex,
                        0, columnCount - 1));
            }
            rowIndex++;
        }

        for (List<HeaderCell> headerRow : exported.headerRows()) {
            Row row = sheet.createRow(rowIndex);
            int column = 0;
            for (HeaderCell headerCell : headerRow) {
                cell(row, column, headerCell.text(), headerStyle);
                for (int i = 1; i < headerCell.span(); i++) {
                    cell(row, column + i, "", headerStyle);
                }
                if (headerCell.span() > 1) {
                    // The grid's joined header cell becomes a merged range —
                    // the whole point of exporting the static parts.
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex,
                            rowIndex, column, column + headerCell.span() - 1));
                }
                column += headerCell.span();
            }
            rowIndex++;
        }

        if (exported.rows().isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            cell(row, 0, exported.emptyStateText(), null);
            rowIndex++;
        }
        for (List<String> dataRow : exported.rows()) {
            Row row = sheet.createRow(rowIndex);
            for (int column = 0; column < dataRow.size(); column++) {
                cell(row, column, dataRow.get(column), null);
            }
            rowIndex++;
        }

        for (List<String> footerRow : exported.footerRows()) {
            Row row = sheet.createRow(rowIndex);
            for (int column = 0; column < footerRow.size(); column++) {
                cell(row, column, footerRow.get(column), footerStyle);
            }
            rowIndex++;
        }

        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            if (sheet.getColumnWidth(column) > MAX_AUTOSIZED_WIDTH) {
                sheet.setColumnWidth(column, MAX_AUTOSIZED_WIDTH);
            }
        }
    }

    private static void cell(Row row, int column, String value,
            @Nullable CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static CellStyle titleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle footerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
}
