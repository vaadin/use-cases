package com.example.uc5;

import java.util.List;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.OrderLine;
import com.example.data.Orders;
import com.example.print.OrderDocument;
import com.example.print.PrintColumns;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — A letterhead on every sheet, and "Page 2 of 4" under it.
 * <p>
 * Two things a printed business document always has, and neither is something
 * the browser will do for a web page. CSS has {@code @page} margin boxes and a
 * {@code counter(page)} for exactly this, but no major browser implements them
 * for content pages, and the browser's own header and footer are the user's
 * setting, not the application's — they carry the URL and the system date, not
 * a company address.
 * <p>
 * So the application paginates. The document is cut into fixed-size sheets
 * server-side, each sheet carries its own copy of the letterhead and its own
 * "Page n of m", and {@code break-after: page} in {@code print.css} puts each
 * one on its own piece of paper. The cost is the one this technique always has:
 * the server has to guess how many lines fit, because it cannot measure the
 * paper.
 * <p>
 * The cheaper alternative, when only a table header needs repeating, is
 * {@code thead { display: table-header-group }} — that is UC3.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Letterhead and page numbers")
@Menu(order = 5, title = "UC5 — Letterhead and page numbers")
@StyleSheet("uc5.css")
public class HeaderFooterView extends VerticalLayout {

    private static final List<Integer> LINES_PER_SHEET = List.of(10, 20, 40);

    private final Div sheets = new Div();
    private final Select<Integer> linesPerSheet = new Select<>();

    public HeaderFooterView() {
        Div intro = new Div();
        intro.addClassName("no-print");
        intro.add(new H1("UC5 — Letterhead and page numbers"));
        intro.add(new Paragraph(
                "A 64-line order, cut into sheets by the server. Every sheet "
                        + "repeats the letterhead and closes with its own "
                        + "page number; the last one carries the total. "
                        + "Change how many lines fit on a sheet and the "
                        + "pagination — and the page numbers — follow."));

        linesPerSheet.setLabel("Lines per sheet");
        linesPerSheet.setItems(LINES_PER_SHEET);
        linesPerSheet.setValue(20);
        linesPerSheet.setId("lines-select");
        linesPerSheet.addValueChangeListener(event -> paginate());

        Button print = new Button("Print", event -> MissingAPI
                .print(event.getSource().getUI().orElseThrow()));
        print.addThemeVariants(ButtonVariant.PRIMARY);
        print.setId("print-button");

        HorizontalLayout controls = new HorizontalLayout(linesPerSheet, print);
        controls.addClassName("no-print");
        controls.setAlignItems(Alignment.END);

        sheets.addClassNames("sheets", "printable");
        sheets.setId("sheets");

        add(intro, controls, sheets);
        paginate();
    }

    private void paginate() {
        Order order = Orders.longOrder(64);
        int perSheet = linesPerSheet.getValue();
        List<OrderLine> lines = order.lines();
        int sheetCount = (lines.size() + perSheet - 1) / perSheet;

        sheets.removeAll();
        for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
            int from = sheetIndex * perSheet;
            int to = Math.min(from + perSheet, lines.size());
            boolean last = sheetIndex == sheetCount - 1;
            sheets.add(sheet(order, lines.subList(from, to), sheetIndex + 1,
                    sheetCount, last));
        }
    }

    private Div sheet(Order order, List<OrderLine> lines, int number, int of,
            boolean last) {
        Div sheet = new Div();
        sheet.addClassName("sheet");
        sheet.setId("sheet-" + number);

        sheet.add(OrderDocument.letterhead(order));
        sheet.add(PrintColumns.asTable(OrderDocument.LINE_COLUMNS, lines));

        if (last) {
            Div total = new Div();
            total.addClassName("doc-total");
            total.add(new Span("Total " + OrderDocument.money(order.total())));
            sheet.add(total);
        }

        Div footer = new Div();
        footer.addClassName("doc-footer");
        footer.add(new Span("Order " + order.id()),
                new Span("Page " + number + " of " + of));
        if (!last) {
            footer.add(new Span("continued overleaf"));
        }
        sheet.add(footer);
        return sheet;
    }
}
