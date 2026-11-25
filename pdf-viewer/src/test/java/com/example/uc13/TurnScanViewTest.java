package com.example.uc13;

import java.util.List;

import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.pdfviewer.PdfViewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@ViewPackages(classes = TurnScanView.class)
class TurnScanViewTest extends SpringBrowserlessTest {

    private ExecutedJavaScript executed;

    @BeforeEach
    void captureJavaScript() {
        executed = ExecutedJavaScript.capture();
    }

    @Test
    void theScanHasASidewaysSecondPage() {
        byte[] scan = SampleDocuments.scannedDeliveryNote().bytes();

        assertEquals(0, PageRotation.rotation(scan, 1));
        assertEquals(90, PageRotation.rotation(scan, 2));
        assertEquals(180,
                PageRotation.rotation(PageRotation.rotate(scan, 2, 90), 2));
        assertEquals(270,
                PageRotation.rotation(PageRotation.rotate(scan, 1, -90), 1));
    }

    @Test
    void turningTheCurrentPageReloadsTheDocumentAtThatPage() {
        navigate(TurnScanView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        PdfViewerTester tester = new PdfViewerTester(viewer, executed);
        tester.load(2, "Delivery Note DN-5521");
        tester.scrollTo(2);
        String src = viewer.getElement().getAttribute("src");

        test(button("Turn left")).click();

        assertNotEquals(src, viewer.getElement().getAttribute("src"));
        // The new document opens at page 1, then the viewer goes back to 2.
        assertEquals(List.of(2), tester.takeScrollToPage());
        tester.scrollTo(2);
        assertEquals("Page 2 is now stored upright.", status());

        test(button("Turn left")).click();
        assertEquals("Page 2 is now stored turned by 270°.", status());
    }

    private Button button(String label) {
        return find(Button.class).from(findInView(PdfViewer.class).single())
                .all().stream().filter(button -> button.getAriaLabel()
                        .orElse("").equals(label))
                .findFirst().orElseThrow();
    }

    private String status() {
        return findInView(Paragraph.class).withClassName("uc13-status").single()
                .getText();
    }
}
