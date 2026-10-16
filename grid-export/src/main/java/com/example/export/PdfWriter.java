package com.example.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.export.ExportedGrid.Alignment;
import com.example.export.ExportedGrid.HeaderCell;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Writes an {@link ExportedGrid} as a paginated PDF table: the report title on
 * the first page, and <strong>every</strong> header row — including the grouped
 * ones — repeated at the top of each page, the way a printed report has to
 * carry them.
 * <p>
 * This is the format that shows why an export API has to hand out the static
 * parts separately from the rows. A CSV or a spreadsheet writes the headers
 * once and streams the rows past them; a paginating writer has to re-emit the
 * headers every time it breaks a page, and it has to measure the rows before it
 * can lay any of them out. See {@code API-GAPS.md}.
 */
public final class PdfWriter {

    /** MIME type for a PDF download. */
    public static final String CONTENT_TYPE = "application/pdf";

    private static final PDFont BODY_FONT = new PDType1Font(FontName.HELVETICA);

    private static final PDFont BOLD_FONT = new PDType1Font(
            FontName.HELVETICA_BOLD);

    private static final float TITLE_SIZE = 14;

    private static final float FONT_SIZE = 8;

    private static final float ROW_HEIGHT = 13;

    private static final float MARGIN = 32;

    /** Space between the title and the header rows below it. */
    private static final float TITLE_GAP = 10;

    /** Space taken by the rule drawn under the last header row. */
    private static final float HEADER_RULE_GAP = 3;

    /** Space between the last data row and the footer rule above the totals. */
    private static final float FOOTER_GAP = 2;

    /** Padding between a cell's text and the next column. */
    private static final float CELL_PADDING = 6;

    private static final String ELLIPSIS = "...";

    private PdfWriter() {
    }

    /** Renders the report as PDF bytes. */
    public static byte[] toBytes(ExportedGrid exported) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new Layout(exported).write(document);
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A download of the PDF. The supplier runs inside the download request, so
     * the file reflects the grid as it is when the user clicks.
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
     * Extracts the text of each page, so a view can show what it produced and a
     * test can assert that the headers really do repeat.
     *
     * @return one entry per page, in page order
     */
    public static List<String> readBackPages(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
            return List.copyOf(pages);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** What a single line of the table is, and how tall it is. */
    private enum LineKind {

        /** An ordinary data row. */
        DATA(ROW_HEIGHT),

        /** A footer row: bold, with a rule and a gap above it. */
        FOOTER(ROW_HEIGHT + FOOTER_GAP),

        /**
         * The empty-state text, in place of the rows the report has none of.
         */
        NOTE(ROW_HEIGHT);

        private final float height;

        LineKind(float height) {
            this.height = height;
        }
    }

    private record Line(List<String> cells, LineKind kind) {
    }

    /**
     * One report's worth of layout: the page geometry, the measured column
     * widths, and the lines split into pages.
     * <p>
     * The split happens <em>before</em> anything is drawn, in
     * {@link #paginate()}, and rendering then just walks the result. That is
     * what keeps "Page 1 of 6" honest: there is only one place that decides
     * where a page breaks, so the stamped total cannot disagree with the number
     * of pages actually produced.
     */
    private static final class Layout {

        private final ExportedGrid exported;

        private final PDRectangle pageSize;

        private final float[] columnWidths;

        private final float[] columnOffsets;

        private final List<List<Line>> pages;

        private @Nullable PDPageContentStream content;

        private float cursorY;

        private Layout(ExportedGrid exported) {
            this.exported = exported;
            // Landscape: a table is wider than it is tall.
            this.pageSize = new PDRectangle(PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth());
            this.columnWidths = measureColumns();
            this.columnOffsets = new float[columnWidths.length];
            float offset = MARGIN;
            for (int i = 0; i < columnWidths.length; i++) {
                columnOffsets[i] = offset;
                offset += columnWidths[i];
            }
            this.pages = paginate();
        }

        /** Every line of the table, in the order it is printed. */
        private List<Line> lines() {
            List<Line> lines = new ArrayList<>();
            if (exported.rows().isEmpty()) {
                lines.add(new Line(List.of(exported.emptyStateText()),
                        LineKind.NOTE));
            }
            exported.rows()
                    .forEach(row -> lines.add(new Line(row, LineKind.DATA)));
            exported.footerRows().forEach(
                    footer -> lines.add(new Line(footer, LineKind.FOOTER)));
            return lines;
        }

        /**
         * Splits the lines into pages. The title only costs height on the first
         * page; the header rows cost it on every one.
         */
        private List<List<Line>> paginate() {
            List<List<Line>> split = new ArrayList<>();
            List<Line> current = new ArrayList<>();
            float y = contentTop(0);
            for (Line line : lines()) {
                if (y - line.kind().height < MARGIN && !current.isEmpty()) {
                    split.add(List.copyOf(current));
                    current.clear();
                    y = contentTop(split.size());
                }
                current.add(line);
                y -= line.kind().height;
            }
            // Always at least one page, even for a report with no lines at all.
            split.add(List.copyOf(current));
            return List.copyOf(split);
        }

        /**
         * Where the first line of a page starts, below the title (first page
         * only) and the repeated header rows. Both {@link #paginate()} and the
         * renderer use this, so they cannot drift apart.
         */
        private float contentTop(int pageIndex) {
            float top = pageSize.getHeight() - MARGIN;
            if (pageIndex == 0 && !exported.title().isEmpty()) {
                top -= TITLE_SIZE + TITLE_GAP;
            }
            top -= exported.headerRows().size() * ROW_HEIGHT;
            return top - HEADER_RULE_GAP;
        }

        private void write(PDDocument document) throws IOException {
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                startPage(document, pageIndex);
                for (Line line : pages.get(pageIndex)) {
                    drawLine(line);
                }
                content().close();
            }
        }

        /** Begins a page and lays down the title and the repeated headers. */
        private void startPage(PDDocument document, int pageIndex)
                throws IOException {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            cursorY = pageSize.getHeight() - MARGIN;

            if (pageIndex == 0 && !exported.title().isEmpty()) {
                drawText(exported.title(), MARGIN, cursorY - TITLE_SIZE,
                        BOLD_FONT, TITLE_SIZE);
                cursorY -= TITLE_SIZE + TITLE_GAP;
            }
            drawHeaderRows();
            drawText("Page " + (pageIndex + 1) + " of " + pages.size(), MARGIN,
                    MARGIN - 12, BODY_FONT, FONT_SIZE);
            cursorY = contentTop(pageIndex);
        }

        /**
         * Draws every header row the grid has, grouped cells included, and
         * underlines the last one. This runs once per page — that is the whole
         * point of the format.
         */
        private void drawHeaderRows() throws IOException {
            for (List<HeaderCell> headerRow : exported.headerRows()) {
                int column = 0;
                for (HeaderCell cell : headerRow) {
                    float width = 0;
                    for (int i = 0; i < cell.span()
                            && column + i < columnWidths.length; i++) {
                        width += columnWidths[column + i];
                    }
                    if (!cell.text().isEmpty()) {
                        drawCell(cell.text(), columnOffsets[column], width,
                                cursorY - ROW_HEIGHT + 4, BOLD_FONT,
                                cell.span() > 1 ? Alignment.CENTER
                                        : exported.alignment(column));
                    }
                    column += cell.span();
                }
                cursorY -= ROW_HEIGHT;
            }
            drawRule(cursorY + HEADER_RULE_GAP);
        }

        private void drawLine(Line line) throws IOException {
            switch (line.kind()) {
            case NOTE -> {
                drawText(line.cells().getFirst(), MARGIN,
                        cursorY - ROW_HEIGHT + 4, BODY_FONT, FONT_SIZE);
                cursorY -= LineKind.NOTE.height;
            }
            case FOOTER -> {
                cursorY -= FOOTER_GAP;
                drawRule(cursorY + FOOTER_GAP);
                drawCells(line.cells(), BOLD_FONT);
                cursorY -= ROW_HEIGHT;
            }
            default -> {
                drawCells(line.cells(), BODY_FONT);
                cursorY -= ROW_HEIGHT;
            }
            }
        }

        private void drawCells(List<String> cells, PDFont font)
                throws IOException {
            for (int column = 0; column < cells.size()
                    && column < columnWidths.length; column++) {
                drawCell(cells.get(column), columnOffsets[column],
                        columnWidths[column], cursorY - ROW_HEIGHT + 4, font,
                        exported.alignment(column));
            }
        }

        private void drawCell(String text, float left, float width, float y,
                PDFont font, Alignment alignment) throws IOException {
            float available = Math.max(0, width - CELL_PADDING);
            String clipped = clip(text, font, available);
            float x = cellTextX(left, available,
                    textWidth(clipped, font, FONT_SIZE), alignment);
            drawText(clipped, x, y, font, FONT_SIZE);
        }

        private void drawText(String text, float x, float y, PDFont font,
                float size) throws IOException {
            if (text.isEmpty()) {
                return;
            }
            content().beginText();
            content().setFont(font, size);
            content().newLineAtOffset(x, y);
            content().showText(sanitize(text));
            content().endText();
        }

        private void drawRule(float y) throws IOException {
            content().setLineWidth(0.5f);
            content().moveTo(MARGIN, y);
            content().lineTo(pageSize.getWidth() - MARGIN, y);
            content().stroke();
        }

        /**
         * The stream of the page being written. Non-null between
         * {@link #startPage} and the matching close.
         */
        private PDPageContentStream content() {
            PDPageContentStream stream = content;
            if (stream == null) {
                throw new IllegalStateException("No page has been started");
            }
            return stream;
        }

        /**
         * Measures every cell to size the columns, then scales them to the page
         * width. The grid cannot answer this: {@code Column#getWidth()} is a
         * CSS string and is {@code null} for an auto-width column, whose real
         * width only exists in the browser.
         */
        private float[] measureColumns() {
            int count = exported.columnCount();
            float[] widths = new float[count];
            for (int column = 0; column < count; column++) {
                widths[column] = textWidth(exported.columnHeaders().get(column),
                        BOLD_FONT, FONT_SIZE);
            }
            List<List<String>> measured = new ArrayList<>(exported.rows());
            measured.addAll(exported.footerRows());
            for (List<String> row : measured) {
                for (int column = 0; column < count
                        && column < row.size(); column++) {
                    widths[column] = Math.max(widths[column],
                            textWidth(row.get(column), BODY_FONT, FONT_SIZE));
                }
            }
            float total = 0;
            for (int column = 0; column < count; column++) {
                widths[column] += CELL_PADDING;
                total += widths[column];
            }
            float available = pageSize.getWidth() - 2 * MARGIN;
            if (total > 0) {
                float scale = available / total;
                for (int column = 0; column < count; column++) {
                    widths[column] *= scale;
                }
            }
            return widths;
        }
    }

    /** Where a cell's text starts, given the alignment of its column. */
    static float cellTextX(float left, float available, float textWidth,
            Alignment alignment) {
        return switch (alignment) {
        case END -> left + available - textWidth;
        case CENTER -> left + (available - textWidth) / 2;
        case START -> left;
        };
    }

    /**
     * Shortens {@code text} until it fits {@code available} points, marking the
     * cut with an ellipsis. A report cannot reflow a table cell, so the
     * alternative would be text running into the next column.
     */
    static String clip(String text, PDFont font, float available) {
        String sanitized = sanitize(text);
        if (textWidth(sanitized, font, FONT_SIZE) <= available) {
            return sanitized;
        }
        String clipped = sanitized;
        while (!clipped.isEmpty()
                && textWidth(clipped + ELLIPSIS, font, FONT_SIZE) > available) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped.isEmpty() ? "" : clipped + ELLIPSIS;
    }

    private static float textWidth(String text, PDFont font, float size) {
        try {
            return font.getStringWidth(sanitize(text)) / 1000 * size;
        } catch (IOException | IllegalArgumentException e) {
            // Fall back to a rough estimate rather than failing the export.
            return text.length() * size * 0.5f;
        }
    }

    /**
     * The standard 14 fonts encode WinAnsi only, so anything outside it — the
     * bullets of a masked card number, a CJK name — has to be replaced rather
     * than thrown at {@code showText}, which would fail the whole export.
     */
    static String sanitize(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            if (character == '…') {
                out.append(ELLIPSIS);
            } else if (character >= 0x20 && character <= 0x7E) {
                out.append(character);
            } else if (character >= 0xA0 && character <= 0xFF) {
                out.append(character);
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }
}
