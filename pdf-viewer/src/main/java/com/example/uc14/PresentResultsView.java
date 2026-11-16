package com.example.uc14;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.pdfviewer.Zoom;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC14 — Present the results.
 * <p>
 * The quarterly results are shown as slides at a meeting. The viewer shows only
 * the pages, each fitted with {@code Zoom.PAGE_FIT}; the app's own bar below it
 * has large "Previous" and "Next" buttons, the slide number and a download
 * button. The bar is bound to {@link PdfViewer#pageSignal()} and
 * {@link PdfViewer#pageCountSignal()}, and its buttons call
 * {@link PdfViewer#scrollToPage(int)} and {@link PdfViewer#download()}, so it
 * also follows when the presenter scrolls the pages.
 * <p>
 * {@link PdfViewer#setHeaderVisible(boolean)} hides the header, but then the
 * viewer always shows its toolbar: there is no API to hide it (API-GAPS.md
 * #15). The view hides it with the {@code toolbar} part in {@code uc14.css}.
 */
@Route(value = "uc14", layout = MainLayout.class)
@PageTitle("UC14 — Present the results")
@UseCaseDescription("Hiding the viewer's toolbar and driving it from the app's own controls")
@Menu(order = 14, title = "UC14 — Present the results")
@StyleSheet("uc14.css")
public class PresentResultsView extends VerticalLayout {

    private final PdfViewer viewer = new PdfViewer();

    public PresentResultsView() {
        addClassName("uc14-view");
        add(new H1("UC14 — Present the results"));
        add(new Paragraph("The quarterly results, shown as slides at a "
                + "meeting. The viewer shows only the slides; the bar below "
                + "it is the app's own. Move with \"Previous\" and \"Next\", "
                + "or scroll the slides: the slide number follows."));

        viewer.addClassName("uc14-viewer");
        viewer.setHeaderVisible(false);
        viewer.setZoom(Zoom.PAGE_FIT);
        viewer.setSrc(SampleDocuments.quarterlyResults().handler());

        Signal<Integer> page = viewer.pageSignal();
        Signal<Integer> pageCount = viewer.pageCountSignal();
        Button previous = new Button("Previous", VaadinIcon.ARROW_LEFT.create(),
                event -> viewer.scrollToPage(viewer.getPage() - 1));
        previous.bindEnabled(() -> pageCount.get() > 0 && page.get() > 1);
        Button next = new Button("Next", VaadinIcon.ARROW_RIGHT.create(),
                event -> viewer.scrollToPage(viewer.getPage() + 1));
        next.setIconAfterText(true);
        next.addThemeVariants(ButtonVariant.PRIMARY);
        next.bindEnabled(() -> page.get() < pageCount.get());
        Span position = new Span();
        position.addClassName("uc14-position");
        position.bindText(() -> pageCount.get() == 0 ? "Loading…"
                : "Slide " + page.get() + " of " + pageCount.get());
        Button download = new Button(VaadinIcon.DOWNLOAD.create(),
                event -> viewer.download());
        download.setAriaLabel("Download");
        download.setTooltipText("Download the slides");
        download.addThemeVariants(ButtonVariant.TERTIARY);
        download.bindEnabled(() -> pageCount.get() > 0);

        Div bar = new Div(previous, position, next, download);
        bar.addClassName("uc14-bar");
        add(viewer, bar);
        add(ApiGap.note(15, "the viewer can hide its header but not its "
                + "toolbar. The view hides it with CSS."));
    }
}
