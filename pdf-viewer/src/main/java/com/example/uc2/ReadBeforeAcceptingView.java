package com.example.uc2;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Read before accepting.
 * <p>
 * Terms, policies and contracts often have to be read before the user may
 * accept them. The view follows {@code page-changed} and remembers the furthest
 * page the user has reached. The checkbox only turns on once that is the last
 * page of the document, whose number comes from {@code document-load}.
 * <p>
 * The viewer reports the page that takes up most of the visible area, so the
 * last page counts as reached when the user has scrolled it into view, not when
 * they have read it. That is what such a rule can promise in any app.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Read before accepting")
@UseCaseDescription("Letting the user accept terms only after scrolling through them")
@Menu(order = 2, title = "UC2 — Read before accepting")
@StyleSheet("uc2.css")
public class ReadBeforeAcceptingView extends VerticalLayout {

    private final PdfViewer viewer = new PdfViewer();
    private final Span progress = new Span();
    private final Checkbox accept = new Checkbox(
            "I have read and accept the terms of service");
    private final Button proceed = new Button("Create account");
    private final Span result = new Span();

    private int furthestPage;

    public ReadBeforeAcceptingView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Read before accepting"));
        add(new Paragraph("Scroll through the terms of service below. The "
                + "checkbox turns on once you have reached the last page. "
                + "Jumping there with the page field counts too: the app sees "
                + "pages, not reading."));

        viewer.addClassName("uc2-viewer");
        viewer.setWidthFull();
        viewer.setSrc(SampleDocuments.termsOfService().handler());
        viewer.addDocumentLoadListener(event -> {
            furthestPage = Math.max(furthestPage, viewer.getPage());
            update();
        });
        viewer.addPageChangeListener(event -> {
            furthestPage = Math.max(furthestPage, event.getPage());
            update();
        });

        progress.addClassName("uc2-progress");
        accept.setEnabled(false);
        accept.addValueChangeListener(event -> update());
        proceed.addThemeVariants(ButtonVariant.PRIMARY);
        proceed.setEnabled(false);
        proceed.addClickListener(event -> result
                .setText("Accepted. The app would now create the account."));

        add(viewer, progress, accept, proceed, result);
        add(ApiGap.note(10, "the Flow component should offer the page as a "
                + "signal, so the furthest page could be a computed signal "
                + "instead of a listener that updates three components."));
        update();
    }

    private void update() {
        int pageCount = viewer.getPageCount();
        boolean reachedEnd = pageCount > 0 && furthestPage >= pageCount;
        if (pageCount == 0) {
            progress.setText("Loading the terms…");
        } else if (reachedEnd) {
            progress.setText("You have reached the last page.");
        } else {
            progress.setText("You have reached page " + furthestPage + " of "
                    + pageCount + ".");
        }
        accept.setEnabled(reachedEnd);
        proceed.setEnabled(reachedEnd && accept.getValue());
    }
}
