package com.example.uc7;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Styling one component.
 * <p>
 * Sometimes a single row, field or card has to stand out, usually because of
 * its data. Four tools cover it, from the most to the least specific:
 * <ul>
 * <li>Grid rows get a part name from their data
 * ({@link Grid#setPartNameGenerator}), which CSS styles with
 * {@code vaadin-grid::part(overdue)}.</li>
 * <li>A class on one field, plus the field's own {@code ::part(input-field)},
 * styles that field only.</li>
 * <li>A component token set inline ({@code --vaadin-button-background}) changes
 * one button without any stylesheet.</li>
 * <li>A class bound to a signal ({@code bindClassName}) makes a card turn red
 * when its value drops below a threshold.</li>
 * </ul>
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Styling one component")
@UseCaseDescription("Highlighting single rows, fields and cards based on their data")
@Menu(order = 7, title = "UC7 — Styling one component")
@StyleSheet("uc7.css")
public class SingleInstanceView extends VerticalLayout {

    record Invoice(String number, String customer, BigDecimal amount,
            LocalDate due, boolean paid) {

        boolean overdue(LocalDate today) {
            return !paid && due.isBefore(today);
        }
    }

    static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    static final List<Invoice> INVOICES = List.of(
            new Invoice("INV-301", "Kestrel Air", new BigDecimal("1240.00"),
                    LocalDate.of(2026, 2, 28), false),
            new Invoice("INV-302", "Blue Finch", new BigDecimal("380.50"),
                    LocalDate.of(2026, 3, 15), false),
            new Invoice("INV-303", "Harbour Foods", new BigDecimal("2210.00"),
                    LocalDate.of(2026, 2, 20), true),
            new Invoice("INV-304", "Northwind", new BigDecimal("95.00"),
                    LocalDate.of(2026, 3, 2), false));

    static final int LOW_STOCK = 10;

    private final Grid<Invoice> grid = new Grid<>();
    private final ValueSignal<Integer> stock = new ValueSignal<>(42);
    private final Div stockCard = new Div();

    public SingleInstanceView() {
        addClassName("uc7-view");

        add(new H1("UC7 — Styling one component"));
        add(new Paragraph("Overdue invoices are red and paid ones grey, from "
                + "their data. One field and one button are styled on their "
                + "own. Lower the stock below " + LOW_STOCK
                + " and its card turns red."));

        grid.addColumn(Invoice::number).setHeader("Invoice");
        grid.addColumn(Invoice::customer).setHeader("Customer");
        grid.addColumn(Invoice::amount).setHeader("Amount");
        grid.addColumn(Invoice::due).setHeader("Due");
        grid.setPartNameGenerator(invoice -> invoice.paid() ? "paid"
                : invoice.overdue(TODAY) ? "overdue" : null);
        grid.setItems(INVOICES);
        grid.setAllRowsVisible(true);

        TextField iban = new TextField("IBAN (check this one)");
        iban.addClassName("needs-attention");
        TextField reference = new TextField("Reference");

        Button approve = new Button("Approve payment");
        approve.getStyle().set("--vaadin-button-background", "#15803d")
                .set("--vaadin-button-text-color", "white");

        IntegerField stockField = new IntegerField("Espresso beans in stock");
        stockField.setStepButtonsVisible(true);
        stockField.setMin(0);
        stockField.bindValue(stock,
                value -> stock.set(value == null ? 0 : value));
        Span stockValue = new Span();
        stockValue.addClassName("stock-value");
        stockValue.bindText(stock.map(String::valueOf));
        stockCard.add(new Span("Stock"), stockValue);
        stockCard.addClassName("stock-card");
        stockCard.bindClassName("low", stock.map(s -> s < LOW_STOCK));

        HorizontalLayout fields = new HorizontalLayout(iban, reference);
        fields.setAlignItems(Alignment.BASELINE);
        HorizontalLayout stockRow = new HorizontalLayout(stockField, stockCard);
        stockRow.setAlignItems(Alignment.END);
        Div sample = new Div(new H2("Payments"), grid, fields, approve,
                stockRow);
        sample.addClassName("sample");
        add(sample);
    }

    // Package-private test seams.
    Grid<Invoice> grid() {
        return grid;
    }

    Div stockCard() {
        return stockCard;
    }

    Signal<Integer> stock() {
        return stock;
    }
}
