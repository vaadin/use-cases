package com.example.print;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.example.data.Order;
import com.example.data.OrderLine;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;

/**
 * The printed order confirmation: letterhead, addressee, the ordered lines and
 * the total. Every use case in this module prints this same document, so that
 * what differs between them is the printing technique and nothing else.
 */
public class OrderDocument extends Div {

    /** The columns of the lines table, shared with the Grid in UC3. */
    public static final List<PrintColumn<OrderLine>> LINE_COLUMNS = List.of(
            PrintColumn.of("Article no.", OrderLine::sku),
            PrintColumn.of("Description", OrderLine::description),
            PrintColumn.numeric("Qty",
                    line -> Integer.toString(line.quantity())),
            PrintColumn.numeric("Unit price", line -> money(line.unitPrice())),
            PrintColumn.numeric("Total", line -> money(line.total())));

    private static final DateTimeFormatter DATE = DateTimeFormatter
            .ofPattern("d MMM yyyy", Locale.ENGLISH);

    /**
     * The whole document: letterhead, addressee, every line of the order and
     * the total. A use case that paginates the order itself builds its sheets
     * from {@link #letterhead(Order)} and
     * {@link PrintColumns#asTable(List, List)} instead.
     *
     * @param order
     *            the order to print
     */
    public OrderDocument(Order order) {
        addClassNames("document");

        add(letterhead(order));

        Div addressee = new Div();
        addressee.addClassName("doc-address");
        order.address().forEach(line -> addressee.add(new Div(line)));
        add(addressee);

        Table table = PrintColumns.asTable(LINE_COLUMNS, order.lines());
        TableRow total = table.addFooterRow();
        total.addColumnHeaderCell("Total").setColspan(4);
        total.addDataCell(money(order.total())).addClassName("numeric");
        add(table);
    }

    /**
     * The letterhead block: seller, order number and order date. Public because
     * the multi-sheet use case repeats it on every sheet.
     *
     * @param order
     *            the order being printed
     * @return a new letterhead for that order
     */
    public static Div letterhead(Order order) {
        Div letterhead = new Div();
        letterhead.addClassName("doc-letterhead");
        letterhead.add(new H2("Kettle & Cup Oy"));
        Div meta = new Div();
        meta.addClassName("doc-meta");
        meta.add(new Span("Order " + order.id()),
                new Span(order.ordered().format(DATE)),
                new Span(order.itemCount() + " items"));
        letterhead.add(meta);
        letterhead.add(new Paragraph("Harbour Road 4, 20100 Turku, Finland"));
        return letterhead;
    }

    /**
     * Formats an amount the same way everywhere, independently of the server's
     * locale, so that tests can assert on it.
     *
     * @param amount
     *            the amount to format
     * @return the amount with two decimals and a euro sign
     */
    public static String money(BigDecimal amount) {
        return String.format(Locale.ROOT, "%,.2f €", amount);
    }
}
