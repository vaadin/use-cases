package com.example.documents;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfBuilderTest {

    @Test
    void theContentsPageLinksEachHeadingToItsPage() throws IOException {
        PdfBuilder builder = new PdfBuilder("Handbook").withContentsPage()
                .chaptersOnNewPages().heading("First").paragraph("One.")
                .heading("Second").paragraph("Two.");
        byte[] bytes = builder.build();
        Map<String, Integer> headingPages = builder.headingPages();
        assertEquals(Map.of("First", 2, "Second", 3), headingPages,
                "the chapters start after the contents page, without an "
                        + "empty page between");

        try (PDDocument document = Loader.loadPDF(bytes)) {
            List<Integer> linkedPages = document.getPage(0).getAnnotations()
                    .stream().map(PDAnnotationLink.class::cast)
                    .map(link -> document.getPages()
                            .indexOf(((PDPageDestination) linkDestination(link))
                                    .getPage())
                            + 1)
                    .toList();
            assertEquals(List.of(2, 3), linkedPages);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(1);
            String contents = stripper.getText(document);
            assertTrue(contents.contains("Contents"), contents);
            assertTrue(contents.contains("First"), contents);
            assertTrue(contents.contains("Second"), contents);
        }
    }

    private static Object linkDestination(PDAnnotationLink link) {
        try {
            return link.getDestination();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
