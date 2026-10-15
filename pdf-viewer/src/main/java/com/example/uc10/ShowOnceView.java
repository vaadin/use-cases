package com.example.uc10;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.example.common.UseCaseDescription;
import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC10 — Show a document once.
 * <p>
 * A confidential briefing may be read once, in the app, and must not be
 * downloaded or printed. Two things make that so: the viewer is shown without
 * its download and print buttons, and {@link OneTimeDocument} sends the file
 * only for the first request. A reload, a second tab or the file's address
 * opened elsewhere gets 410 Gone, and the view says that the briefing was
 * opened already.
 * <p>
 * The viewer has no property to hide its buttons, so {@code uc10.css} hides
 * them (API-GAPS.md #7). What happens in the browser can not be fully
 * controlled: the reader can still select and copy the text, use the browser's
 * own print command or take a screenshot (API-GAPS.md #13).
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Show a document once")
@UseCaseDescription("Showing a confidential document once, without download or print")
@Menu(order = 10, title = "UC10 — Show a document once")
@StyleSheet("uc10.css")
public class ShowOnceView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private final OneTimeDocument document;
    private final Pdf briefing = SampleDocuments.boardBriefing();
    private final Button open = new Button("Open the briefing");
    private final Paragraph status = new Paragraph();
    private final PdfViewer viewer = new PdfViewer();

    public ShowOnceView(OneTimeDocument document) {
        this.document = document;
        addClassName("uc10-view");
        add(new H1("UC10 — Show a document once"));
        add(new Paragraph("The board briefing is confidential. You may read "
                + "it once, here in the app, and it must not be downloaded or "
                + "printed. Open it, then reload this page: it does not open "
                + "again."));

        open.addThemeVariants(ButtonVariant.PRIMARY);
        open.addClickListener(event -> openBriefing());
        status.addClassName("uc10-status");
        viewer.addClassName("uc10-viewer");
        viewer.setVisible(false);
        viewer.addDocumentLoadListener(event -> showOpened());
        viewer.addDocumentErrorListener(event -> {
            viewer.setVisible(false);
            showStatus();
        });
        add(open, status, viewer);

        Button reset = new Button("Let me open it again", event -> {
            document.reset();
            viewer.setVisible(false);
            showStatus();
        });
        Div demo = new Div(new H2("Demo controls"),
                new Paragraph("A real app would not offer this. It forgets "
                        + "that you opened the briefing, so you can try "
                        + "again."),
                reset);
        demo.addClassName("uc10-demo");
        add(demo);

        add(ApiGap.note(7, "the viewer has no property to hide its download "
                + "and print buttons. This view hides them with CSS."));
        add(ApiGap.note(13, "the text can still be selected and copied, and "
                + "the browser's own print command still prints the page."));
        showStatus();
    }

    private void openBriefing() {
        viewer.setSrc(document.handler(briefing));
        viewer.setVisible(true);
        open.setEnabled(false);
        status.setText("Opening…");
    }

    private void showOpened() {
        Instant openedAt = document.openedAt();
        status.setText("Opened at " + format(openedAt)
                + " UTC. The server will "
                + "not send the file again: reload this page, or open the "
                + "file's address in a new tab, and it is gone.");
    }

    private void showStatus() {
        Instant openedAt = document.openedAt();
        open.setEnabled(openedAt == null);
        status.setText(openedAt == null ? "Not opened yet."
                : "You opened the briefing at " + format(openedAt) + " UTC"
                        + ". It can not be opened again.");
    }

    private static String format(@Nullable Instant instant) {
        return instant == null ? "—" : TIME.format(instant);
    }
}
