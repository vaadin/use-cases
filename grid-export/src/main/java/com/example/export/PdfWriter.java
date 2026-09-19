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

    /**
     * One report's worth of layout state: the page geometry, the measured
     * column widths, and a cursor that walks down the current page.
     */
    private static final class Layout {

        private final ExportedGrid exported;

        private final PDRectangle pageSize;

        private final float[] columnWidths;

        private final float[] columnOffsets;

        private final int totalPages;

        private @Nullable PDPageContentStream content;

        private float cursorY;

        private int pageNumber;

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
            this.totalPages = countPages();
        }

        private void write(PDDocument document) throws IOException {
            startPage(document);
            for (List<String> row : exported.rows()) {
                if (!fits(ROW_HEIGHT)) {
                    endPage();
                    startPage(document);
                }
                drawRow(row, BODY_FONT);
            }
            if (exported.rows().isEmpty()) {
                drawText(exported.emptyStateText(), MARGIN,
                        cursorY - ROW_HEIGHT, BODY_FONT, FONT_SIZE);
                cursorY -= ROW_HEIGHT;
            }
            for (List<String> footer : exported.footerRows()) {
                if (!fits(ROW_HEIGHT * 2)) {
                    endPage();
                    startPage(document);
                }
                cursorY -= 2;
                drawLine(cursorY + ROW_HEIGHT - 3);
                drawRow(footer, BOLD_FONT);
            }
            endPage();
        }

        /** Begins a page and lays down the title and the repeated headers. */
        private void startPage(PDDocument document) throws IOException {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;
            cursorY = pageSize.getHeight() - MARGIN;

            if (pageNumber == 1 && !exported.title().isEmpty()) {
                drawText(exported.title(), MARGIN, cursorY - TITLE_SIZE,
                        BOLD_FONT, TITLE_SIZE);
                cursorY -= TITLE_SIZE + 10;
            }
            drawHeaderRows();
            drawPageNumber();
        }

        /**
         * The stream of the page being written. Non-null between
         * {@link #startPage} and {@link #endPage}.
         */
        private PDPageContentStream content() {
            PDPageContentStream stream = content;
            if (stream == null) {
                throw new IllegalStateException("No page has been started");
            }
            return stream;
        }

        private void endPage() throws IOException {
            content().close();
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
            drawLine(cursorY + 3);
            cursorY -= 3;
        }

        private void drawRow(List<String> row, PDFont font) throws IOException {
            for (int column = 0; column < row.size()
                    && column < columnWidths.length; column++) {
                drawCell(row.get(column), columnOffsets[column],
                        columnWidths[column], cursorY - ROW_HEIGHT + 4, font,
                        exported.alignment(column));
            }
            cursorY -= ROW_HEIGHT;
        }

        private void drawCell(String text, float left, float width, float y,
                PDFont font, Alignment alignment) throws IOException {
            float available = Math.max(0, width - CELL_PADDING);
            String clipped = clip(text, font, available);
            float textWidth = textWidth(clipped, font, FONT_SIZE);
            float x = switch (alignment) {
            case END -> left + available - textWidth;
            case CENTER -> left + (available - textWidth) / 2;
            default -> left;
            };
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

        private void drawLine(float y) throws IOException {
            content().setLineWidth(0.5f);
            content().moveTo(MARGIN, y);
            content().lineTo(pageSize.getWidth() - MARGIN, y);
            content().stroke();
        }

        private void drawPageNumber() throws IOException {
            drawText("Page " + pageNumber + " of " + totalPages, MARGIN,
                    MARGIN - 12, BODY_FONT, FONT_SIZE);
        }

        private boolean fits(float height) {
            return cursorY - height > MARGIN;
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
            for (List<String> row : exported.rows()) {
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

        /**
         * How many pages the report needs. Known up front because the row
         * height is fixed — which is also why the rows have to be counted
         * before the first one can be drawn, so that "Page 1 of 12" is
         * truthful.
         */
        private int countPages() {
            float firstPageTop = pageSize.getHeight() - MARGIN
                    - (exported.title().isEmpty() ? 0 : TITLE_SIZE + 10);
            float headerHeight = exported.headerRows().size() * ROW_HEIGHT + 3;
            int rows = Math.max(1, exported.rows().size())
                    + exported.footerRows().size();
            int firstPageRows = rowsThatFit(firstPageTop - headerHeight);
            if (rows <= firstPageRows) {
                return 1;
            }
            int perPage = rowsThatFit(
                    pageSize.getHeight() - MARGIN - headerHeight);
            return 1 + (int) Math
                    .ceil((rows - firstPageRows) / (double) perPage);
        }

        private int rowsThatFit(float top) {
            return Math.max(1, (int) ((top - MARGIN) / ROW_HEIGHT));
        }
    }

    private static String clip(String text, PDFont font, float available) {
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
     * The standard 14 fonts encode WinAnsi only, so anything outside it — a
     * bullet from a masked card number, a CJK name — has to be replaced rather
     * than thrown at {@code showText}.
     */
    private static String sanitize(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            if (character == '…') {
                out.append("...");
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
