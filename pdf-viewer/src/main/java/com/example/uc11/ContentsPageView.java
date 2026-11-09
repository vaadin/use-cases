package com.example.uc11;

import java.util.Map;

import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.Manual;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC11 — Go to a chapter.
 * <p>
 * The employee handbook starts with a contents page whose entries are links to
 * the chapters, as many PDF documents do instead of bookmarks. The viewer does
 * not follow such links: it draws the pages without their links (API-GAPS.md
 * #9).
 * <p>
 * The application generated the handbook, so it knows which page each chapter
 * starts on. The view offers the chapters in its own select, which sets the
 * viewer's page through {@code MissingAPI} (API-GAPS.md #12), and a "Back to
 * contents" button, which sets the page to 1. The chapter of the current page
 * is bound to {@link PdfViewer#pageSignal()}.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Go to a chapter")
@UseCaseDescription("Moving through a long document by its chapters")
@Menu(order = 11, title = "UC11 — Go to a chapter")
@StyleSheet("uc11.css")
public class ContentsPageView extends VerticalLayout {

    static final String CONTENTS = "Contents";

    private final PdfViewer viewer = new PdfViewer();

    public ContentsPageView() {
        addClassName("uc11-view");
        add(new H1("UC11 — Go to a chapter"));
        add(new Paragraph("The handbook's first page lists its chapters, but "
                + "the viewer does not follow the links on it. Pick a chapter "
                + "above the viewer to go there instead. The bar shows the "
                + "chapter you are reading, and \"Back to contents\" takes "
                + "you back to the list."));

        Manual handbook = SampleDocuments.employeeHandbook();
        Map<String, Integer> chapterPages = handbook.chapterPages();

        Select<Map.Entry<String, Integer>> goTo = new Select<>();
        goTo.setAriaLabel("Go to a chapter");
        goTo.setPlaceholder("Go to a chapter");
        goTo.setItems(chapterPages.entrySet());
        goTo.setItemLabelGenerator(Map.Entry::getKey);
        goTo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                MissingAPI.setPage(viewer, event.getValue().getValue());
                goTo.clear();
            }
        });
        Span chapter = new Span();
        chapter.addClassName("uc11-chapter");
        chapter.bindText(
                viewer.pageSignal().map(page -> chapterAt(chapterPages, page)));
        Button back = new Button("Back to contents",
                VaadinIcon.ARROW_UP.create(),
                event -> MissingAPI.setPage(viewer, 1));
        Div bar = new Div(goTo, new Span("You are reading: "), chapter, back);
        bar.addClassName("uc11-bar");

        viewer.addClassName("uc11-viewer");
        viewer.setSrc(handbook.pdf().handler());
        add(bar, viewer);
        add(ApiGap.note(9, "the viewer does not follow the links inside a "
                + "document, so the app offers the chapters itself."));
    }

    /**
     * Finds the chapter a page belongs to: the last one that starts on or
     * before it.
     *
     * @param chapterPages
     *            the page each chapter starts on, in document order
     * @param page
     *            the page, starting from 1
     * @return the chapter, or "Contents" before the first one
     */
    static String chapterAt(Map<String, Integer> chapterPages, int page) {
        String found = CONTENTS;
        for (Map.Entry<String, Integer> entry : chapterPages.entrySet()) {
            if (entry.getValue() <= page) {
                found = entry.getKey();
            }
        }
        return found;
    }
}
