package com.example.uc9;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC9 — Visual regression.
 * <p>
 * Some bugs only show in pixels: a theme update that shifts a column, a CSS
 * change that clips the total, a font that no longer loads. A screenshot test
 * renders the invoice and compares it with a reference image checked in with
 * the code; a difference fails the test and leaves the new image and a diff
 * next to it.
 * <p>
 * Screenshots need stable content. The invoice uses fixed data, and the one
 * part that changes on every load (the time it was printed) is marked
 * {@code volatile} and masked before comparing.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Visual regression")
@UseCaseDescription("Catching layout and styling regressions with screenshot comparison")
@Menu(order = 9, title = "UC9 — Visual regression")
@StyleSheet("uc9.css")
@AnonymousAllowed
public class InvoiceView extends VerticalLayout {

    record Line(String item, String quantity, String amount) {
    }

    public InvoiceView() {
        addClassName("uc9-view");
        add(new H1("UC9 — Visual regression"));
        add(new Paragraph("The invoice below is compared pixel by pixel with "
                + "a reference screenshot. The print time changes on every "
                + "load, so the tests mask it."));

        Grid<Line> lines = new Grid<>();
        lines.addColumn(Line::item).setHeader("Item").setFlexGrow(3);
        lines.addColumn(Line::quantity).setHeader("Qty");
        lines.addColumn(Line::amount).setHeader("Amount");
        lines.setItems(
                List.of(new Line("Espresso beans, 1 kg", "12", "298,80 €"),
                        new Line("Burr grinder", "1", "189,00 €"),
                        new Line("Paper cups, box of 1000", "3", "116,25 €")));
        lines.setAllRowsVisible(true);

        Span printed = new Span("Printed at " + LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        printed.addClassName("volatile");
        Div total = new Div(new Span("Total"), new Span("604,05 €"));
        total.addClassName("invoice-total");

        Div invoice = new Div(new H2("Invoice INV-2026-0042"),
                new Paragraph("Kestrel Air · Hangar 3 · 00560 Helsinki"), lines,
                total, printed);
        invoice.addClassNames("sample", "invoice");
        invoice.setId("invoice");
        add(invoice,
                new TestsNote("TestBench: uc9/InvoiceScreenshotIT",
                        "Playwright: uc9/InvoiceScreenshotPlaywrightIT",
                        "Reference images: src/test/screenshots"));
    }
}
