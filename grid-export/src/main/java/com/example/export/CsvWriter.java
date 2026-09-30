package com.example.export;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.example.export.ExportedGrid.HeaderCell;

import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Writes an {@link ExportedGrid} as RFC 4180 CSV — the "pure data, no
 * formatting" format from use case [1] of
 * <a href="https://github.com/vaadin/platform/issues/7196">vaadin/platform
 * #7196</a>.
 */
public final class CsvWriter {

    /** MIME type browsers expect for a CSV download. */
    public static final String CONTENT_TYPE = "text/csv;charset=UTF-8";

    private CsvWriter() {
    }

    /** Renders the whole report as one CSV string. */
    public static String toCsv(ExportedGrid exported) {
        StringBuilder out = new StringBuilder();
        if (!exported.title().isEmpty()) {
            out.append(escape(exported.title())).append('\n');
        }
        for (List<HeaderCell> headerRow : exported.headerRows()) {
            // A spanning header cell occupies its first column and leaves the
            // covered ones empty; CSV has no concept of a merged cell.
            List<String> cells = headerRow.stream()
                    .flatMap(cell -> Stream.concat(Stream.of(cell.text()),
                            Stream.generate(() -> "").limit(cell.span() - 1L)))
                    .toList();
            appendRow(out, cells);
        }
        if (exported.rows().isEmpty()) {
            out.append(escape(exported.emptyStateText())).append('\n');
        }
        exported.rows().forEach(row -> appendRow(out, row));
        exported.footerRows().forEach(row -> appendRow(out, row));
        return out.toString();
    }

    /**
     * Writes a header row and a lazily-consumed stream of data rows straight to
     * {@code out}, without ever holding the whole report in memory.
     *
     * @return the number of data rows written
     */
    public static long write(Appendable out, List<String> headers,
            Stream<List<String>> rows) {
        try {
            if (!headers.isEmpty()) {
                out.append(toLine(headers)).append('\n');
            }
            long count = 0;
            for (List<String> row : (Iterable<List<String>>) rows::iterator) {
                out.append(toLine(row)).append('\n');
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A download of the full report — headers and footers included. The
     * supplier runs inside the download request, so the file reflects the
     * grid's filtering, sorting and selection at the moment the user clicks,
     * not at the moment the link was built.
     */
    public static DownloadHandler download(String fileName,
            Supplier<ExportedGrid> report) {
        return event -> {
            event.setFileName(fileName);
            event.setContentType(CONTENT_TYPE);
            PrintWriter writer = event.getWriter();
            writer.write(toCsv(report.get()));
            writer.flush();
        };
    }

    /**
     * A download that generates the CSV while the browser is reading it: the
     * suppliers run inside the download request, so the rows can be fetched
     * page by page and written straight to the response.
     */
    public static DownloadHandler streamingDownload(String fileName,
            Supplier<List<String>> headers,
            Supplier<Stream<List<String>>> rows) {
        return event -> {
            event.setFileName(fileName);
            event.setContentType(CONTENT_TYPE);
            PrintWriter writer = event.getWriter();
            write(writer, headers.get(), rows.get());
            writer.flush();
        };
    }

    /** The report as UTF-8 bytes. */
    public static byte[] toBytes(ExportedGrid exported) {
        return toCsv(exported).getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder out, List<String> cells) {
        out.append(toLine(cells)).append('\n');
    }

    /**
     * One CSV line: the cells escaped and comma-separated, without a newline.
     */
    public static String toLine(List<String> cells) {
        return cells.stream().map(CsvWriter::escape)
                .reduce((left, right) -> left + "," + right).orElse("");
    }

    static String escape(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
