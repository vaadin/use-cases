package com.example.uc2;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Read before accepting.
 * <p>
 * Terms, policies and contracts often have to be read before the user may
 * accept them. The view remembers the furthest page the user has reached, from
 * the viewer's page change events. Whether that is the last page is a computed
 * signal over it and {@link PdfViewer#pageCountSignal()}, which the progress
 * text, the checkbox and the button are bound to.
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
    private final ValueSignal<Integer> furthestPage = new ValueSignal<>(1);
    private final ValueSignal<Boolean> accepted = new ValueSignal<>(false);

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
        viewer.addPageChangeListener(event -> furthestPage
                .update(furthest -> Math.max(furthest, event.getPage())));

        Signal<Integer> pageCount = viewer.pageCountSignal();
        Signal<Boolean> reachedEnd = Signal.computed(() -> pageCount.get() > 0
                && furthestPage.get() >= pageCount.get());

        Span progress = new Span();
        progress.addClassName("uc2-progress");
        progress.bindText(() -> progress(furthestPage.get(), pageCount.get()));
        Checkbox accept = new Checkbox(
                "I have read and accept the terms of service");
        accept.bindValue(accepted, accepted::set);
        accept.bindEnabled(reachedEnd);
        Span result = new Span();
        Button proceed = new Button("Create account", event -> result
                .setText("Accepted. The app would now create the account."));
        proceed.addThemeVariants(ButtonVariant.PRIMARY);
        proceed.bindEnabled(() -> reachedEnd.get() && accepted.get());

        add(viewer, progress, accept, proceed, result);
    }

    static String progress(int furthestPage, int pageCount) {
        if (pageCount == 0) {
            return "Loading the terms…";
        }
        if (furthestPage >= pageCount) {
            return "You have reached the last page.";
        }
        return "You have reached page " + furthestPage + " of " + pageCount
                + ".";
    }
}
