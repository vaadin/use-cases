package com.example.uc7;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Finds the page a phrase is on, by extracting the text of each page with
 * PDFBox on the server.
 * <p>
 * The workaround for a viewer that can not search from code (API-GAPS.md #4).
 * It works for any document with a text layer, including ones the application
 * did not generate, but it only finds the page: the viewer can not highlight
 * the phrase.
 */
final class ClauseFinder {

    private final List<String> pages;

    /**
     * Extracts the text of each page.
     *
     * @param pdf
     *            the PDF document
     */
    ClauseFinder(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(normalize(stripper.getText(document)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the document", e);
        }
    }

    /**
     * Finds the first page that contains the phrase. Line breaks and case do
     * not matter, as a phrase may wrap onto the next line.
     *
     * @param phrase
     *            the text to find
     * @return the 1-based page, or empty when no page contains the phrase
     */
    OptionalInt pageOf(String phrase) {
        String needle = normalize(phrase);
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).contains(needle)) {
                return OptionalInt.of(i + 1);
            }
        }
        return OptionalInt.empty();
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
