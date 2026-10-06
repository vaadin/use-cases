package com.example.uc1;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.Orders;
import com.example.print.OrderDocument;
import com.example.print.PrintEvents;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Print the page the user is looking at.
 * <p>
 * The simplest printing there is: a Print button on an ordinary view. Three
 * things have to be arranged by hand, and none of them has a Flow API.
 * <ul>
 * <li>The dialog is opened with {@code window.print()} through
 * {@link MissingAPI#print(com.vaadin.flow.component.UI)} — {@code Page} has no
 * {@code print()}.</li>
 * <li>The application shell — navbar, drawer, this view's own buttons — must be
 * kept off the paper. {@code print.css} does that with a {@code .printable}
 * marker class and {@code .no-print} opt-outs, because a component cannot
 * declare that it is chrome rather than content.</li>
 * <li>The internal note below is on screen but never on paper unless the user
 * asks for it, which is a per-element decision the same CSS class carries.</li>
 * </ul>
 * The status line is fed by {@link PrintEvents}, i.e. by the browser's
 * {@code beforeprint} / {@code afterprint} events relayed to the server. It is
 * the only way the application can know a document was printed at all — and
 * even then it cannot know whether the user printed it, saved a PDF or
 * cancelled.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Print the current view")
@Menu(order = 1, title = "UC1 — Print the current view")
@StyleSheet("uc1.css")
public class PrintCurrentViewView extends VerticalLayout {

    private final Span status = new Span("Not printed yet");
    private final PrintEvents printEvents = new PrintEvents();
    private final Div internalNote = new Div();

    private int printCount;

    public PrintCurrentViewView() {
        addClassName("uc1-view");

        Div intro = new Div();
        intro.addClassName("no-print");
        intro.add(new H1("UC1 — Print the current view"));
        intro.add(new Paragraph(
                "Press Print and the order confirmation below goes to the "
                        + "printer on its own — without the navigation bar, "
                        + "the drawer, this paragraph or the buttons. "
                        + "Everything outside the document is marked as "
                        + "chrome in print.css; nothing about it is automatic."));

        Button print = new Button("Print", event -> MissingAPI
                .print(event.getSource().getUI().orElseThrow()));
        print.addThemeVariants(ButtonVariant.PRIMARY);
        print.setId("print-button");

        Checkbox includeNote = new Checkbox("Print the internal note too");
        includeNote.setId("include-note");
        includeNote.addValueChangeListener(event -> internalNote
                .setClassName("no-print", !event.getValue()));

        status.setId("print-status");
        status.addClassName("status-badge");

        HorizontalLayout controls = new HorizontalLayout(print, includeNote,
                status);
        controls.addClassName("no-print");
        controls.setAlignItems(Alignment.CENTER);

        Order order = Orders.sampleOrder();
        OrderDocument document = new OrderDocument(order);
        document.addClassName("printable");
        document.setId("order-document");

        internalNote.addClassNames("internal-note", "no-print");
        internalNote.setId("internal-note");
        internalNote.add(new Span(
                "Internal: customer is on 14-day payment terms, do not "
                        + "enclose a reminder."));
        document.add(internalNote);

        add(intro, controls, document, printEvents);

        printEvents.addBeforePrintListener(() -> status.setText("Printing…"));
        printEvents.addAfterPrintListener(this::printFinished);
    }

    private void printFinished() {
        printCount++;
        status.setText("Print dialog closed " + printCount
                + (printCount == 1 ? " time" : " times")
                + " — the browser does not say what the user chose");
    }
}
