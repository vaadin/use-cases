package com.example.uc4;

import java.util.List;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.Manual;
import com.example.pdfviewer.PdfViewer;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * UC4 — Open the manual at a page.
 * <p>
 * Help links point into a long user guide at the page that answers the
 * question. The page is a query parameter, {@code uc4?page=5}: the view reads
 * it in {@code beforeEnter} and sets the viewer's page, so a link opens the
 * guide at the right place, from inside the app or from an email. While the
 * user scrolls, the view writes the current page back into the URL with
 * {@code replaceState}, so reloading or sharing the address keeps the place.
 * <p>
 * The application generated the guide, so it knows which page each chapter
 * starts on. The guide also has bookmarks, which the viewer's sidebar shows as
 * an outline.
 * <p>
 * A link past the last page is corrected to the last page once the guide has
 * loaded, because the viewer does not do that itself.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Open the manual at a page")
@UseCaseDescription("Linking into a long user guide at the page that answers a question")
@Menu(order = 4, title = "UC4 — Open the manual at a page")
@StyleSheet("uc4.css")
public class ManualDeepLinkView extends VerticalLayout
        implements BeforeEnterObserver {

    /** A question and the chapter of the guide that answers it. */
    record HelpTopic(String question, String chapter) {
    }

    static final List<HelpTopic> TOPICS = List.of(
            new HelpTopic("How do I reset my password?",
                    "Signing in and passwords"),
            new HelpTopic("How does the timer work?", "Recording time"),
            new HelpTopic("How do I submit my week?", "Submitting a timesheet"),
            new HelpTopic("Can I approve several timesheets at once?",
                    "Approving timesheets"),
            new HelpTopic("How do I export a report to Excel?",
                    "Exporting data"),
            new HelpTopic("Which keyboard shortcuts are there?",
                    "Keyboard shortcuts"));

    private final PdfViewer viewer = new PdfViewer();
    private final Span address = new Span();

    public ManualDeepLinkView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Open the manual at a page"));
        add(new Paragraph("Each help question links into the user guide at "
                + "the page that answers it. Click one, then scroll the guide: "
                + "the address below, and in the browser's address bar, "
                + "follows the page. Reload the page or open the address in "
                + "a new tab and the guide opens where you were."));

        Manual manual = SampleDocuments.userGuide();
        Map<String, Integer> chapterPages = manual.chapterPages();

        Div topics = new Div(new H2("Help"));
        topics.addClassName("uc4-topics");
        for (HelpTopic topic : TOPICS) {
            RouterLink link = new RouterLink(topic.question(),
                    ManualDeepLinkView.class);
            link.setQueryParameters(pageParameter(
                    chapterPages.getOrDefault(topic.chapter(), 1)));
            topics.add(link);
        }
        address.addClassName("uc4-address");
        topics.add(new Paragraph(new Span("Address of this page: "), address));

        viewer.addClassName("uc4-viewer");
        viewer.setSidebarOpened(true);
        viewer.setSrc(manual.pdf().handler());
        viewer.addPageChangeListener(event -> {
            if (event.isFromClient()) {
                replaceAddress(event.getPage());
            }
        });
        // A link to a page past the end leaves the viewer on page 1 with the
        // page property unchanged and an empty page field (API-GAPS.md #11),
        // so the view corrects it once it knows the number of pages.
        viewer.addDocumentLoadListener(event -> {
            if (viewer.getPage() > event.getPageCount()) {
                viewer.setPage(event.getPageCount());
                replaceAddress(event.getPageCount());
            }
        });

        Div layout = new Div(topics, viewer);
        layout.addClassNames("uc4-layout", "side-by-side");
        add(layout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int page = event.getLocation().getQueryParameters()
                .getSingleParameter("page").map(ManualDeepLinkView::parsePage)
                .orElse(1);
        viewer.setPage(page);
        showAddress(page);
    }

    /**
     * Reads the page from the query parameter. Anything that is not a page
     * number opens the first page rather than failing.
     */
    static int parsePage(String value) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void replaceAddress(int page) {
        getUI().ifPresent(ui -> ui.getPage().getHistory().replaceState(null,
                location(page)));
        showAddress(page);
    }

    private void showAddress(int page) {
        address.setText(location(page).getPathWithQueryParameters());
    }

    private static Location location(int page) {
        return new Location("uc4", pageParameter(page));
    }

    private static QueryParameters pageParameter(int page) {
        return QueryParameters.of("page", String.valueOf(page));
    }
}
