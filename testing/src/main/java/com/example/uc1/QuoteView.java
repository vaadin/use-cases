package com.example.uc1;

import java.text.NumberFormat;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.orders.Product;
import com.example.orders.QuoteCalculator;
import com.example.orders.QuoteCalculator.Quote;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC1 — Logic without the UI.
 * <p>
 * The view is a thin shell: it reads three inputs and shows the {@link Quote}
 * that {@link QuoteCalculator} returns. All the rules (volume discounts,
 * business discount, free shipping) live in the calculator, a plain class that
 * a JUnit test checks in milliseconds without starting Spring or Vaadin. The
 * view test only checks the wiring.
 * <p>
 * That is the cheapest and most precise layer of testing, and the more logic it
 * holds, the fewer slow UI tests an application needs.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Logic without the UI")
@UseCaseDescription("Testing business rules with plain unit tests, no UI involved")
@Menu(order = 1, title = "UC1 — Logic without the UI")
@AnonymousAllowed
public class QuoteView extends VerticalLayout {

    private final ValueSignal<Product> product = new ValueSignal<>(
            Product.ESPRESSO_BEANS);
    private final ValueSignal<Integer> quantity = new ValueSignal<>(12);
    private final ValueSignal<Boolean> business = new ValueSignal<>(false);
    private final Span total = new Span();

    public QuoteView(QuoteCalculator calculator) {
        add(new H1("UC1 — Logic without the UI"));
        add(new Paragraph("Change the product, the quantity or the customer "
                + "type: the price comes from a calculator class that is "
                + "unit-tested on its own."));

        Select<Product> productSelect = new Select<>();
        productSelect.setLabel("Product");
        productSelect.setItems(Product.values());
        productSelect.setItemLabelGenerator(Product::title);
        productSelect.bindValue(product, value -> {
            if (value != null) {
                product.set(value);
            }
        });
        IntegerField quantityField = new IntegerField("Quantity");
        quantityField.setMin(1);
        quantityField.setStepButtonsVisible(true);
        quantityField.bindValue(quantity,
                value -> quantity.set(value == null ? 1 : value));
        Checkbox businessBox = new Checkbox("Business customer");
        businessBox.bindValue(business, business::set);

        NumberFormat euros = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        total.addClassName("quote-total");
        total.bindText(Signal.computed(() -> {
            Integer amount = quantity.get();
            if (amount < 1) {
                return "Enter a quantity of at least 1";
            }
            Quote quote = calculator.quote(product.get(), amount,
                    business.get());
            return "Subtotal %s · %d%% off %s · shipping %s · total %s"
                    .formatted(euros.format(quote.subtotal()),
                            quote.discountPercent(),
                            euros.format(quote.discount()),
                            euros.format(quote.shipping()),
                            euros.format(quote.total()));
        }));

        Div sample = new Div(
                new HorizontalLayout(productSelect, quantityField, businessBox),
                total);
        sample.addClassName("sample");
        add(sample, new TestsNote(
                "Unit test: orders/QuoteCalculatorTest (plain JUnit, no Spring)",
                "View test: uc1/QuoteViewTest (browserless)",
                "Run: mvn -pl testing -am test"));
    }

    // Package-private test seam.
    String total() {
        return total.getText();
    }
}
