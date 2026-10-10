package com.example.uc4;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.function.SignalComputation;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Numbers and currency.
 * <p>
 * An order with prices, a VAT percentage and a yearly order count, formatted
 * for the user's language: decimal and grouping separators, the position of the
 * currency symbol, even the digits (Arabic as used in Egypt writes ١٬٢٣٤٫٥٠).
 * <p>
 * The currency is part of the data, not of the language: an order in euros
 * stays in euros for an American user, only its formatting changes. Japanese
 * yen show that the currency also decides the number of decimals.
 * <p>
 * For input, a {@link BigDecimalField} reads the user's decimal separator,
 * while a {@link NumberField} always expects a dot.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Numbers and currency")
@UseCaseDescription("Formatting and entering numbers, percentages and money per locale")
@Menu(order = 4, title = "UC4 — Numbers and currency")
public class NumbersAndCurrencyView extends VerticalLayout {

    record OrderLine(String product, int quantity, BigDecimal unitPrice) {
        BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    static final List<OrderLine> LINES = List.of(
            new OrderLine("Espresso beans, 1 kg", 12, new BigDecimal("24.90")),
            new OrderLine("Grinder service", 1, new BigDecimal("149.00")),
            new OrderLine("Paper cups, box of 1000", 3,
                    new BigDecimal("38.75")));
    static final BigDecimal VAT_RATE = new BigDecimal("0.255");
    static final long ORDERS_THIS_YEAR = 1_234_567;
    static final List<Currency> CURRENCIES = List.of(
            Currency.getInstance("EUR"), Currency.getInstance("USD"),
            Currency.getInstance("JPY"));

    private final ValueSignal<Currency> currency = new ValueSignal<>(
            CURRENCIES.getFirst());
    private final Grid<OrderLine> grid = new Grid<>();
    private final Column<OrderLine> productColumn;
    private final Column<OrderLine> quantityColumn;
    private final Column<OrderLine> priceColumn;
    private final Column<OrderLine> totalColumn;
    private final Select<Currency> currencySelect = new Select<>();
    private final BigDecimalField priceField = new BigDecimalField();
    private final NumberField numberField = new NumberField();
    private final ValueSignal<@Nullable BigDecimal> priceValue = new ValueSignal<>(
            null);
    private final ValueSignal<@Nullable Double> numberValue = new ValueSignal<>(
            null);

    public NumbersAndCurrencyView() {
        UI ui = UI.getCurrent();

        add(new H1("UC4 — Numbers and currency"));
        add(new Paragraph("Switch the language to see the separators, the "
                + "currency symbol and even the digits change. Switch the "
                + "currency to see that it is part of the order, not of the "
                + "language. Type 1234,5 into both fields in German: only the "
                + "decimal field understands it."));

        currencySelect.setItems(CURRENCIES);
        currencySelect.setItemLabelGenerator(Currency::getCurrencyCode);
        currencySelect.bindValue(currency, currency::set);

        productColumn = grid.addColumn(OrderLine::product);
        quantityColumn = grid.addColumn(
                line -> integers(ui.getLocale()).format(line.quantity()))
                .setTextAlign(ColumnTextAlign.END);
        priceColumn = grid.addColumn(line -> money(line.unitPrice(),
                ui.getLocale(), currency.peek()))
                .setTextAlign(ColumnTextAlign.END);
        totalColumn = grid.addColumn(
                line -> money(line.total(), ui.getLocale(), currency.peek()))
                .setTextAlign(ColumnTextAlign.END);
        grid.setItems(LINES);
        grid.setAllRowsVisible(true);

        BigDecimal subtotal = LINES.stream().map(OrderLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = subtotal.multiply(VAT_RATE);
        UnorderedList totals = new UnorderedList(
                line(() -> getTranslation(ui.localeSignal().get(),
                        "uc4.subtotal", money(ui, subtotal))),
                line(() -> {
                    Locale locale = ui.localeSignal().get();
                    NumberFormat percent = NumberFormat
                            .getPercentInstance(locale);
                    percent.setMaximumFractionDigits(1);
                    return getTranslation(locale, "uc4.vat",
                            percent.format(VAT_RATE), money(ui, vat));
                }), line(() -> getTranslation(ui.localeSignal().get(),
                        "uc4.total", money(ui, subtotal.add(vat)))),
                line(() -> {
                    Locale locale = ui.localeSignal().get();
                    return getTranslation(locale, "uc4.orders-year",
                            integers(locale).format(ORDERS_THIS_YEAR),
                            NumberFormat
                                    .getCompactNumberInstance(locale,
                                            NumberFormat.Style.SHORT)
                                    .format(ORDERS_THIS_YEAR));
                }));
        totals.addClassName("totals");

        priceField.bindValue(priceValue, priceValue::set);
        numberField.bindValue(numberValue, numberValue::set);
        Span parsed = new Span();
        parsed.addClassName("parsed");
        parsed.bindText(Signal.computed(() -> "BigDecimalField → "
                + priceValue.get() + " · NumberField → " + numberValue.get()));

        HorizontalLayout inputs = new HorizontalLayout(priceField, numberField);
        Div sample = new Div(currencySelect, grid, totals, inputs, parsed);
        sample.addClassName("sample");
        add(sample);

        Signal.effect(this, () -> {
            Locale locale = ui.localeSignal().get();
            currency.get();
            localize(locale);
        });
    }

    private void localize(Locale locale) {
        currencySelect.setLabel(getTranslation(locale, "uc4.currency"));
        productColumn.setHeader(getTranslation(locale, "uc4.product"));
        quantityColumn.setHeader(getTranslation(locale, "uc4.quantity"));
        priceColumn.setHeader(getTranslation(locale, "uc4.price"));
        totalColumn.setHeader(getTranslation(locale, "uc4.line-total"));
        // The Grid has no idea that its cells depend on the locale.
        grid.getDataProvider().refreshAll();

        priceField.setLabel(getTranslation(locale, "uc4.price-input")
                + " (BigDecimalField)");
        numberField.setLabel(
                getTranslation(locale, "uc4.price-input") + " (NumberField)");
        // Only reads the locale when it is created.
        priceField.setLocale(locale);
    }

    /** Formats an amount in the current language and currency, reactively. */
    private String money(UI ui, BigDecimal amount) {
        return money(amount, ui.localeSignal().get(), currency.get());
    }

    static String money(BigDecimal amount, Locale locale, Currency currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(currency);
        // The currency decides the decimals: two for euros, none for yen.
        // setCurrency leaves them as the locale's own currency has them.
        format.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        format.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        return format.format(amount);
    }

    private static NumberFormat integers(Locale locale) {
        return NumberFormat.getIntegerInstance(locale);
    }

    private static ListItem line(SignalComputation<String> text) {
        Span span = new Span();
        span.bindText(Signal.computed(text));
        return new ListItem(span);
    }

    // Package-private test seams.
    Grid<OrderLine> grid() {
        return grid;
    }

    Select<Currency> currencySelect() {
        return currencySelect;
    }

    BigDecimalField priceField() {
        return priceField;
    }
}
