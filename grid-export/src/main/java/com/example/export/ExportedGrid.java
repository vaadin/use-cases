package com.example.export;

import java.util.List;

/**
 * Everything a report generator needs from a Grid, extracted once by
 * {@link GridExport} and handed to a writer such as {@link CsvWriter},
 * {@link XlsxWriter} or {@link PdfWriter}.
 * <p>
 * This is the "output" half of the facade proposed in
 * <a href="https://github.com/vaadin/platform/issues/7196">vaadin/platform
 * #7196</a>: rows, headers, footers, the empty-state text and the column order,
 * all as plain text, with no Grid types leaking into the writers.
 *
 * @param title
 *            report title, or an empty string
 * @param headerRows
 *            header rows, topmost first; a cell spanning several columns
 *            carries its span
 * @param columnHeaders
 *            the bottom header row flattened to one label per exported column
 * @param alignments
 *            one alignment per exported column, taken from
 *            {@code Column#getTextAlign()} so a report can right-align the
 *            columns the grid right-aligns
 * @param rows
 *            the data rows, each with one cell per exported column
 * @param footerRows
 *            footer rows, each with one cell per exported column
 * @param emptyStateText
 *            what the grid shows when it has no rows, so an empty report says
 *            the same thing
 */
public record ExportedGrid(String title, List<List<HeaderCell>> headerRows,
        List<String> columnHeaders, List<Alignment> alignments,
        List<List<String>> rows, List<List<String>> footerRows,
        String emptyStateText) {

    /**
     * One header cell and the number of exported columns it covers.
     *
     * @param text
     *            the header text
     * @param span
     *            how many columns the cell spans; 1 for an ordinary header
     */
    public record HeaderCell(String text, int span) {
    }

    /**
     * How a column's cells are aligned, mapped from {@code ColumnTextAlign} so
     * that the writers stay free of Grid types.
     */
    public enum Alignment {
        START, CENTER, END
    }

    /** The number of exported columns. */
    public int columnCount() {
        return columnHeaders.size();
    }

    /** The alignment of column {@code index}, {@code START} if unknown. */
    public Alignment alignment(int index) {
        return index < alignments.size() ? alignments.get(index)
                : Alignment.START;
    }
}
