package com.example.uc7;

import java.util.OptionalInt;

import com.example.documents.PdfBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClauseFinderTest {

    private final PdfBuilder builder = new PdfBuilder("Contract")
            .chaptersOnNewPages().heading("1. Scope")
            .paragraph("The supplier provides the services.")
            .heading("2. Payment")
            .paragraph("Invoices are paid within thirty days of their "
                    + "date, by bank transfer to the account named on the "
                    + "invoice, without any deduction or set-off whatsoever.")
            .heading("3. Notices").paragraph("Notices are given in writing.");

    @Test
    void findsThePageOfAPhrase() {
        ClauseFinder finder = new ClauseFinder(builder.build());
        assertEquals(OptionalInt.of(builder.headingPages().get("2. Payment")),
                finder.pageOf("within thirty days"));
        assertEquals(OptionalInt.of(builder.headingPages().get("3. Notices")),
                finder.pageOf("NOTICES ARE GIVEN"));
    }

    @Test
    void findsAPhraseThatWrapsOntoTheNextLine() {
        ClauseFinder finder = new ClauseFinder(builder.build());
        // The paragraph is wider than the page, so it wraps somewhere in the
        // middle; the whole sentence is still found.
        assertEquals(OptionalInt.of(builder.headingPages().get("2. Payment")),
                finder.pageOf("by bank transfer to the account named on the "
                        + "invoice, without any deduction or set-off"));
    }

    @Test
    void reportsAPhraseThatIsNotThere() {
        assertEquals(OptionalInt.empty(),
                new ClauseFinder(builder.build()).pageOf("arbitration"));
    }
}
