package com.example.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.export.ExportedGrid.Alignment;
import com.example.export.ExportedGrid.HeaderCell;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PDF writing rules, away from any Grid: pagination, the character
 * substitution the standard fonts force, cell clipping and alignment.
 */
class PdfWriterTest {

    private static final PDFont FONT = new PDType1Font(FontName.HELVETICA);

    /**
     * Every page has to be stamped with the number of pages that were actually
     * produced. The row counts below straddle the page breaks — including the
     * ones where the footer rows are what tips the report onto an extra page.
     */
    @ParameterizedTest
    @CsvSource({ "0,0", "0,1", "1,0", "1,1", "35,1", "36,1", "37,1", "200,1",
            "222,1", "223,1", "225,1", "223,3", "225,3", "400,3" })
    void everyPageIsStampedWithTheRealPageCount(int rowCount, int footerCount) {
        byte[] pdf = PdfWriter.toBytes(report(rowCount, footerCount));

        List<String> pages = PdfWriter.readBackPages(pdf);
        for (int page = 0; page < pages.size(); page++) {
            assertTrue(
                    pages.get(page).contains(
                            "Page " + (page + 1) + " of " + pages.size()),
                    rowCount + " rows and " + footerCount
                            + " footer rows produced " + pages.size()
                            + " pages, but page " + (page + 1)
                            + " is not stamped with that total");
        }
    }

    @Test
    void theHeaderRowsCostHeightOnEveryPageSoLaterPagesHoldFewerRows() {
        // 400 rows over a landscape A4 with a title and one header row: the
        // first page loses height to the title, the rest do not.
        List<String> pages = PdfWriter
                .readBackPages(PdfWriter.toBytes(report(400, 1)));

        assertTrue(pages.size() > 5,
                "400 rows should need several pages, got " + pages.size());
        for (String page : pages) {
            assertTrue(page.contains("Name"),
                    "the header row is missing from a page");
        }
    }

    @Test
    void anEmptyReportCarriesTheEmptyStateTextOnItsSinglePage() {
        List<String> pages = PdfWriter
                .readBackPages(PdfWriter.toBytes(report(0, 0)));

        assertEquals(1, pages.size());
        assertTrue(pages.getFirst().contains("Nothing to report."),
                "an empty report should say what the grid says: "
                        + pages.getFirst());
    }

    @Test
    void charactersTheStandardFontsCannotEncodeAreReplaced() {
        assertEquals("plain", PdfWriter.sanitize("plain"));
        // Latin-1 survives: the middle dot UC4 uses to join two properties.
        assertEquals("Ada · FI", PdfWriter.sanitize("Ada · FI"));
        // The bullets UC6 masks card numbers with do not.
        assertEquals("???? ???? ???? 1234",
                PdfWriter.sanitize("•••• •••• •••• 1234"));
        assertEquals("...", PdfWriter.sanitize("…"));
        assertEquals("??", PdfWriter.sanitize("東京"));
        assertEquals("a?b", PdfWriter.sanitize("a\nb"));
    }

    @Test
    void aReportFullOfUnencodableCharactersStillExports() {
        ExportedGrid report = new ExportedGrid("Masked cards",
                List.of(List.of(new HeaderCell("Card", 1))), List.of("Card"),
                List.of(Alignment.START),
                List.of(List.of("•••• •••• •••• 4321"), List.of("東京")),
                List.of(), "Nothing to report.");

        // The point is that this does not throw: showText rejects anything the
        // font cannot encode, which would fail the whole export.
        String text = String.join("\n",
                PdfWriter.readBackPages(PdfWriter.toBytes(report)));

        assertTrue(text.contains("???? ???? ???? 4321"), text);
        assertFalse(text.contains("•"), text);
    }

    @Test
    void textWiderThanItsCellIsClippedWithAnEllipsis() {
        String text = "Engineering department, northern region";
        float full = width(text);

        assertEquals(text, PdfWriter.clip(text, FONT, full + 1),
                "text that fits is left alone");

        String clipped = PdfWriter.clip(text, FONT, full / 2);
        assertTrue(clipped.endsWith("..."), clipped);
        assertTrue(clipped.length() < text.length(), clipped);
        assertTrue(width(clipped) <= full / 2,
                "the clipped text must fit the cell: " + clipped);
        assertTrue(text.startsWith(clipped.substring(0, 5)),
                "clipping should keep the start of the text: " + clipped);
    }

    @Test
    void aCellTooNarrowForAnythingStaysEmpty() {
        assertEquals("", PdfWriter.clip("Engineering", FONT, 0));
        assertEquals("", PdfWriter.clip("Engineering", FONT, -5));
        assertEquals("", PdfWriter.clip("Engineering", FONT, 1));
    }

    @Test
    void cellTextIsPlacedAccordingToTheColumnsAlignment() {
        assertEquals(100, PdfWriter.cellTextX(100, 60, 20, Alignment.START));
        assertEquals(140, PdfWriter.cellTextX(100, 60, 20, Alignment.END));
        assertEquals(120, PdfWriter.cellTextX(100, 60, 20, Alignment.CENTER));
    }

    @Test
    void anEndAlignedColumnIsDrawnFurtherRightThanAStartAlignedOne() {
        float start = valueX(Alignment.START);
        float end = valueX(Alignment.END);

        assertTrue(start < end,
                "an END-aligned cell should sit right of a START-aligned one, "
                        + "got " + start + " and " + end);
        assertTrue(start < 40,
                "a START-aligned cell should sit at the left margin, got "
                        + start);
    }

    /** The x of the single data cell, for a one-column report. */
    private static float valueX(Alignment alignment) {
        ExportedGrid report = new ExportedGrid("",
                List.of(List.of(new HeaderCell("Header", 1))),
                List.of("Header"), List.of(alignment), List.of(List.of("ZZ")),
                List.of(), "Nothing to report.");
        return textPositions(PdfWriter.toBytes(report)).get("ZZ");
    }

    private static float width(String text) {
        try {
            return FONT.getStringWidth(text) / 1000 * 8;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The left edge of each string drawn in the document, by its text. */
    private static Map<String, Float> textPositions(byte[] pdf) {
        Map<String, Float> positions = new HashMap<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text,
                        List<TextPosition> characters) {
                    if (!characters.isEmpty()) {
                        positions.putIfAbsent(text.strip(),
                                characters.getFirst().getX());
                    }
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
            return positions;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A report of {@code rowCount} rows and {@code footerCount} footer rows,
     * with a title and one header row.
     */
    private static ExportedGrid report(int rowCount, int footerCount) {
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < rowCount; row++) {
            rows.add(List.of("Employee " + row, "Engineering"));
        }
        List<List<String>> footers = new ArrayList<>();
        for (int footer = 0; footer < footerCount; footer++) {
            footers.add(List.of("Total " + footer, String.valueOf(rowCount)));
        }
        return new ExportedGrid("Report",
                List.of(List.of(new HeaderCell("Name", 1),
                        new HeaderCell("Department", 1))),
                List.of("Name", "Department"),
                List.of(Alignment.START, Alignment.END), rows, footers,
                "Nothing to report.");
    }
}
