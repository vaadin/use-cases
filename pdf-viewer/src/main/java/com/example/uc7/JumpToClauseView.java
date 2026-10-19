package com.example.uc7;

import java.util.List;
import java.util.OptionalInt;

import com.example.common.UnreleasedApi;
import com.example.common.UseCaseDescription;
import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.ScrollIntoViewOption;
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
 * UC7 — Jump to a clause.
 * <p>
 * A contract arrives from the other party; the reviewer has a checklist of
 * questions and wants the clause that answers each one. The application did not
 * write the contract and does not know its layout, so it searches the text:
 * {@link ClauseFinder} extracts each page's text with PDFBox on the server, and
 * the view sets the viewer's page to the first match.
 * <p>
 * That is as far as the API goes. The viewer has a find bar that highlights
 * matches, but no API to fill it in or to highlight text from code, so the
 * reviewer still has to find the sentence on the page.
 */
@UnreleasedApi("https://github.com/vaadin/web-components/pull/13053")
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Jump to a clause")
@UseCaseDescription("Opening a contract at the clause that answers a question")
@Menu(order = 7, title = "UC7 — Jump to a clause")
@StyleSheet("uc7.css")
public class JumpToClauseView extends VerticalLayout {

    /** A question of the review checklist and the words that answer it. */
    record Question(String text, String phrase) {
    }

    static final List<Question> CHECKLIST = List.of(
            new Question("How long do we have to pay an invoice?",
                    "within 30 days of its date"),
            new Question("How can we get out of the contract?",
                    "6. Termination"),
            new Question("What is the most we can claim?", "total liability"),
            new Question("How fast must a data breach be reported?",
                    "within 36 hours"),
            new Question("Which courts decide a dispute?", "courts of Dublin"));

    private final PdfViewer viewer = new PdfViewer();
    private final Paragraph result = new Paragraph();

    public JumpToClauseView() {
        addClassName("uc7-view");
        add(new H1("UC7 — Jump to a clause"));
        add(new Paragraph("Pick a question from the review checklist. The app "
                + "searches the contract on the server and opens it at the "
                + "page with the answer. To see the exact words highlighted, "
                + "click into the viewer, press Ctrl+F (Cmd+F on a Mac) and "
                + "type them."));

        Pdf agreement = SampleDocuments.serviceAgreement();
        ClauseFinder finder = new ClauseFinder(agreement.bytes());

        Div checklist = new Div(new H2("Review checklist"));
        checklist.addClassName("uc7-checklist");
        for (Question question : CHECKLIST) {
            Button button = new Button(question.text(),
                    event -> jump(finder, question));
            button.addThemeVariants(ButtonVariant.TERTIARY);
            checklist.add(button);
        }
        result.addClassName("uc7-result");
        checklist.add(result);

        viewer.addClassName("uc7-viewer");
        viewer.setSrc(agreement.handler());

        Div layout = new Div(checklist, viewer);
        layout.addClassNames("uc7-layout", "side-by-side");
        add(layout);
        add(ApiGap.note(4,
                "the viewer can not search or highlight text from "
                        + "code. The app finds the page itself and leaves the "
                        + "sentence to the reader."));
    }

    private void jump(ClauseFinder finder, Question question) {
        OptionalInt page = finder.pageOf(question.phrase());
        if (page.isEmpty()) {
            result.setText("\"" + question.phrase() + "\" is not in this "
                    + "contract.");
            return;
        }
        viewer.setPage(page.getAsInt());
        // On a phone the viewer is below the checklist.
        viewer.scrollIntoView(ScrollIntoViewOption.Block.NEAREST);
        result.setText("Look for \"" + question.phrase() + "\" on page "
                + page.getAsInt() + ".");
    }
}
