package com.example.uc3;

import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DocumentLibraryView.class)
class DocumentLibraryViewTest extends SpringBrowserlessTest {

    @Test
    void aLoadedFileIsSummarized() {
        navigate(DocumentLibraryView.class);
        viewer().load(1, "Travel Policy");
        assertEquals("Opened: 1 page, titled \"Travel Policy\".", message());
        viewer().load(3, "");
        assertEquals("Opened: 3 pages.", message());
    }

    @ParameterizedTest
    @CsvSource({ "2, invalid, damaged or is not a PDF",
            "3, network, could not be fetched",
            "4, password, protected with a password" })
    void eachReasonGetsItsOwnExplanation(int row, String reason,
            String expected) {
        navigate(DocumentLibraryView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        String firstSrc = viewer.getElement().getAttribute("src");
        @SuppressWarnings("unchecked")
        Grid<DocumentLibraryView.LibraryFile> grid = findInView(Grid.class)
                .single();
        test(grid).select(row);
        assertNotEquals(firstSrc, viewer.getElement().getAttribute("src"));

        new PdfViewerTester(viewer).fail(reason);

        assertTrue(message().contains(expected), message());
    }

    @Test
    void aProtectedFileCanStillBeOpenedInTheBrowser() {
        navigate(DocumentLibraryView.class);
        @SuppressWarnings("unchecked")
        Grid<DocumentLibraryView.LibraryFile> grid = findInView(Grid.class)
                .single();
        test(grid).select(4);
        viewer().fail("password");

        Anchor open = findInView(Anchor.class)
                .withText("Open it in the browser instead").single();
        assertTrue(open.getHref().endsWith("payslip-2026-09.pdf"),
                open.getHref());
    }

    private PdfViewerTester viewer() {
        return new PdfViewerTester(findInView(PdfViewer.class).single());
    }

    private String message() {
        return findInView(Div.class).withClassName("uc3-message").single()
                .getElement().getTextRecursively();
    }
}
