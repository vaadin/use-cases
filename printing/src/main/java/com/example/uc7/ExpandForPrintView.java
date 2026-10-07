package com.example.uc7;

import java.util.List;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.Orders;
import com.example.print.OrderDocument;
import com.example.print.PrintColumns;
import com.example.print.PrintTrigger;
import com.example.print.RenderNowAction;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC7 — Expand every collapsed section for print.
 * <p>
 * On screen, an order is easier to read with its sections folded away; on
 * paper, a folded section is simply missing. The content of a closed
 * {@link Details} is hidden inside its shadow root, so a print stylesheet
 * cannot reveal it, and setting {@code opened} from a server-side
 * {@code beforeprint} listener arrives after the browser has already laid out
 * the pages.
 * <p>
 * So the change has to happen in the browser, inside the event. Each section
 * gets a pair of {@link PrintTrigger}s: on {@code beforeprint} it remembers
 * whether it was open, opens, and renders that at once with a
 * {@link RenderNowAction} — the Print button starts printing from script, where
 * the component would otherwise render too late for the paper. On
 * {@code afterprint} it goes back to what it was. No round trip, no
 * hand-written JavaScript in the view, and the user's own choice of open
 * sections survives printing.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Expand everything for print")
@Menu(order = 7, title = "UC7 — Expand everything for print")
public class ExpandForPrintView extends VerticalLayout {

    /**
     * Where a section keeps its on-screen state while it is printed. A plain
     * JavaScript property on the element, never synchronised to the server.
     */
    private static final String OPENED_ON_SCREEN = "openedOnScreen";

    public ExpandForPrintView() {
        Div intro = new Div();
        intro.addClassName("no-print");
        intro.add(new H1("UC7 — Expand everything for print"));
        intro.add(new Paragraph(
                "Fold or unfold the sections of the order below as you like, "
                        + "then press Print: every section is on the paper, "
                        + "and the page is back the way you left it when the "
                        + "dialog closes."));

        Button print = new Button("Print", event -> MissingAPI
                .print(event.getSource().getUI().orElseThrow()));
        print.addThemeVariants(ButtonVariant.PRIMARY);
        print.setId("print-button");
        print.addClassName("no-print");

        Order order = Orders.sampleOrder();

        Div address = new Div();
        address.addClassName("doc-address");
        order.address().forEach(line -> address.add(new Div(line)));

        Div document = new Div();
        document.addClassNames("document", "printable");
        document.setId("order-document");
        document.add(OrderDocument.letterhead(order));

        List<Details> sections = List.of(
                section("delivery", "Delivery address", address),
                section("lines", "Order lines",
                        PrintColumns.asTable(OrderDocument.LINE_COLUMNS,
                                order.lines())),
                section("terms", "Delivery and returns",
                        new Paragraph("Delivered within five working days. "
                                + "Unopened articles can be returned within "
                                + "30 days of delivery for a full refund."),
                        new Paragraph("Payment is due 14 days from the "
                                + "invoice date.")));
        sections.getFirst().setOpened(true);
        sections.forEach(section -> {
            expandWhilePrinting(section);
            document.add(section);
        });

        add(intro, print, document);
    }

    private static Details section(String id, String summary,
            Component... content) {
        Details details = new Details(summary, content);
        details.setId(id);
        return details;
    }

    private static void expandWhilePrinting(Details section) {
        PrintTrigger.beforePrint(section)
                .triggers(
                        new SetPropertyAction<>(section, OPENED_ON_SCREEN,
                                new PropertyInput<>(section, "opened",
                                        Boolean.class)),
                        new SetPropertyAction<>(section, "opened", true),
                        new RenderNowAction(section));
        PrintTrigger.afterPrint(section).triggers(new SetPropertyAction<>(
                section, "opened",
                new PropertyInput<>(section, OPENED_ON_SCREEN, Boolean.class)));
    }
}
