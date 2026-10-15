package com.example.uc13;

import com.example.common.UseCaseDescription;
import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC13 — Turn a sideways scan.
 * <p>
 * The second page of a scanned delivery note went through the scanner sideways.
 * The viewer can not turn a page (API-GAPS.md #16), so the view adds "Turn
 * left" and "Turn right" buttons to the viewer's toolbar
 * ({@code PdfViewer#addToToolbar}, API-GAPS.md #7). They turn the current page
 * on the server with PDFBox, by changing the page's rotation, and show the
 * document again at the same page.
 * <p>
 * Turning on the server also fixes the file: whoever opens it next sees the
 * page upright. A real application would save the turned file in place of the
 * scan; this view keeps it for as long as it is open.
 */
@Route(value = "uc13", layout = MainLayout.class)
@PageTitle("UC13 — Turn a sideways scan")
@UseCaseDescription("Turning a page that was scanned sideways, and keeping it turned")
@Menu(order = 13, title = "UC13 — Turn a sideways scan")
@StyleSheet("uc13.css")
public class TurnScanView extends VerticalLayout {

    private final PdfViewer viewer = new PdfViewer();
    private final Paragraph status = new Paragraph();
    private Pdf scan = SampleDocuments.scannedDeliveryNote();
    private int pageAfterLoad;

    public TurnScanView() {
        addClassName("uc13-view");
        add(new H1("UC13 — Turn a sideways scan"));
        add(new Paragraph("The second page of this scanned delivery note is "
                + "sideways. Scroll to it and turn it with the two buttons the "
                + "app adds to the viewer's toolbar. The page is turned "
                + "in the file itself, so a real app could save it and the "
                + "next reader would see it upright. This demo keeps the "
                + "turned file only while the view is open."));

        Button left = turnButton(VaadinIcon.ROTATE_LEFT, "Turn left",
                "Turn the current page counterclockwise", -90);
        Button right = turnButton(VaadinIcon.ROTATE_RIGHT, "Turn right",
                "Turn the current page clockwise", 90);
        viewer.addToToolbar(left, right);
        viewer.addClassName("uc13-viewer");
        viewer.setZoom(PdfViewer.Zoom.PAGE_FIT);
        viewer.setSrc(scan.handler());
        // The viewer keeps a page that is set together with a new src, but
        // Flow does not send a page equal to the one it knows already, so the
        // new document would start at page 1. Go back to the page once the
        // document has loaded.
        viewer.addDocumentLoadListener(event -> {
            if (pageAfterLoad > 0) {
                viewer.setPage(pageAfterLoad);
                pageAfterLoad = 0;
            }
        });
        status.addClassName("uc13-status");
        status.setText("Page 2 is stored turned by 90°.");

        add(viewer, status);
        add(ApiGap.note(16, "the viewer can not turn a page, so the app turns "
                + "it on the server and loads the document again."));
    }

    private Button turnButton(VaadinIcon icon, String label, String tooltip,
            int degrees) {
        Button button = new Button(icon.create(), event -> turn(degrees));
        button.setAriaLabel(label);
        button.setTooltipText(tooltip);
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }

    private void turn(int degrees) {
        int page = viewer.getPage();
        scan = new Pdf(scan.fileName(),
                PageRotation.rotate(scan.bytes(), page, degrees));
        viewer.setSrc(scan.handler());
        pageAfterLoad = page;
        int rotation = PageRotation.rotation(scan.bytes(), page);
        status.setText(
                "Page " + page + " is now stored " + (rotation == 0 ? "upright."
                        : "turned by " + rotation + "°."));
    }
}
