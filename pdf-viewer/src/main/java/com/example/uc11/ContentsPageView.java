package com.example.uc11;

import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.Manual;
import com.example.pdfviewer.PdfViewer;
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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC11 — Follow the contents page.
 * <p>
 * The employee handbook starts with a contents page whose entries are links to
 * the chapters, as many PDF documents do instead of bookmarks. The viewer
 * follows such links itself: a click scrolls to the chapter.
 * <p>
 * The viewer does not remember where a link was followed from, has no "back"
 * and does not tell the application that a link was followed (API-GAPS.md #14).
 * The view therefore shows the chapter of the current page from
 * {@code page-changed}, and offers its own "Back to contents" button, which
 * sets the page to 1.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Follow the contents page")
@UseCaseDescription("Moving through a document with the links on its contents page")
@Menu(order = 11, title = "UC11 — Follow the contents page")
@StyleSheet("uc11.css")
public class ContentsPageView extends VerticalLayout {

    static final String CONTENTS = "Contents";

    private final PdfViewer viewer = new PdfViewer();
    private final Span chapter = new Span();

    public ContentsPageView() {
        addClassName("uc11-view");
        add(new H1("UC11 — Follow the contents page"));
        add(new Paragraph("The handbook's first page lists its chapters. "
                + "Click one to go there. The bar above the viewer shows the "
                + "chapter you are reading, and \"Back to contents\" takes you "
                + "back to the list."));

        Manual handbook = SampleDocuments.employeeHandbook();
        Map<String, Integer> chapterPages = handbook.chapterPages();

        chapter.addClassName("uc11-chapter");
        chapter.setText(CONTENTS);
        Button back = new Button("Back to contents",
                VaadinIcon.ARROW_UP.create(), event -> viewer.setPage(1));
        Div bar = new Div(new Span("You are reading: "), chapter, back);
        bar.addClassName("uc11-bar");

        viewer.addClassName("uc11-viewer");
        viewer.setSrc(handbook.pdf().handler());
        viewer.addPageChangeListener(event -> chapter
                .setText(chapterAt(chapterPages, event.getPage())));
        add(bar, viewer);
        add(ApiGap.note(14,
                "after following a link, the viewer has no way "
                        + "back, and it does not tell the app that a link was "
                        + "followed."));
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
