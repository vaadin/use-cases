package com.example.usecase06;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-06", layout = MainLayout.class)
@PageTitle("Use Case 6: Shopping Cart with Real-time Totals")
@Menu(order = 6, title = "UC6 — Shopping Cart")
@UseCaseDescription("Keeping cart totals in sync with every change")
@StyleSheet("usecase06.css")
@AnonymousAllowed
public class UseCase06View extends VerticalLayout {

    public UseCase06View() {
        addClassName("usecase06-view");
        setSpacing(true);
        setPadding(true);

        var title = new H2("Use Case 6: Shopping Cart with Real-time Totals");

        var description = new Paragraph(
                "This use case demonstrates a shopping cart with reactive totals. "
                        + "As you adjust quantities, add/remove items, enter discount codes, or change shipping options, "
                        + "all totals (subtotal, discount, tax, shipping, and grand total) update automatically. "
                        + "Try using discount codes SAVE10 or SAVE20.");

        // Create signals for cart state
        var cartItemsSignal = new ListSignal<CartItem>();
        var discountCodeSignal = new ValueSignal<>("");
        var shippingOptionSignal = new ValueSignal<>(ShippingOption.STANDARD);

        // Computed signal for subtotal
        var subtotalSignal = Signal.computed(
                () -> cartItemsSignal.getValues().map(CartItem::totalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Computed signal for discount
        var discountSignal = Signal.computed(() -> {
            String code = discountCodeSignal.get();
            DiscountCode discount = validateDiscountCode(code);
            if (discount != null) {
                return subtotalSignal.get().multiply(discount.percentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        });

        // Computed signal for shipping cost
        var shippingSignal = shippingOptionSignal.map(this::getShippingCost);

        // Computed signal for tax (8%)
        var taxSignal = Signal.computed(() -> subtotalSignal.get()
                .subtract(discountSignal.get()).multiply(new BigDecimal("0.08"))
                .setScale(2, RoundingMode.HALF_UP));

        // Computed signal for grand total
        var totalSignal = Signal.computed(() -> subtotalSignal.get()
                .subtract(discountSignal.get()).add(shippingSignal.get())
                .add(taxSignal.get()).setScale(2, RoundingMode.HALF_UP));

        var products = List.of(
                new Product("1", "Laptop", new BigDecimal("999.99")),
                new Product("2", "Mouse", new BigDecimal("25.99")),
                new Product("3", "Keyboard", new BigDecimal("79.99")),
                new Product("4", "Monitor", new BigDecimal("249.50")));

        // Products list
        var productsTitle = new H3("Available Products");
        var productsContainer = new Div();
        productsContainer.addClassName("products-container");
        products.forEach(product -> productsContainer
                .add(createProductRow(product, cartItemsSignal)));

        // Cart items display
        var cartTitle = new H3("Shopping Cart");
        var emptyCartButton = new Button("Empty cart",
                e -> cartItemsSignal.clear());
        emptyCartButton.addThemeName("error");
        emptyCartButton.addThemeName("small");

        var cartHeader = new HorizontalLayout(cartTitle, emptyCartButton);
        cartHeader.setWidthFull();
        cartHeader.setAlignItems(Alignment.CENTER);
        cartHeader.expand(cartTitle);
        var cartItemsContainer = new Div();
        cartItemsContainer.addClassName("cart-items-container");

        var cartItemsList = new Div();
        cartItemsList.bindChildren(cartItemsSignal,
                itemSignal -> createCartItemRow(itemSignal, cartItemsSignal));

        var emptyCart = new Paragraph("Empty cart");
        emptyCart.addClassName("empty-cart-text");
        emptyCart.bindVisible(cartItemsSignal.map(List::isEmpty));
        cartItemsContainer.add(emptyCart, cartItemsList);

        // Options section
        var optionsLayout = new HorizontalLayout();
        optionsLayout.setWidthFull();
        optionsLayout.setSpacing(true);

        var discountField = new TextField("Discount Code");
        discountField.setPlaceholder("Enter SAVE10 or SAVE20");
        discountField.setWidth("250px");
        discountField.bindValue(discountCodeSignal, discountCodeSignal::set);

        var shippingSelect = new ComboBox<>("Shipping Method",
                ShippingOption.values());
        shippingSelect.setValue(ShippingOption.STANDARD);
        shippingSelect.setWidth("200px");
        shippingSelect.bindValue(shippingOptionSignal,
                shippingOptionSignal::set);

        optionsLayout.add(discountField, shippingSelect);

        // Totals display
        var totalsBox = new Div();
        totalsBox.addClassName("totals-box");

        var summaryTitle = new H3("Order Summary");
        summaryTitle.addClassName("summary-title");

        var subtotalLabel = new Span(formatter("Subtotal: $", subtotalSignal));
        subtotalLabel.addClassName("total-line");

        var discountLabel = new Span(formatter("Discount: -$", discountSignal));
        discountLabel.bindVisible(
                () -> discountSignal.get().compareTo(BigDecimal.ZERO) > 0);
        discountLabel.addClassName("total-line");
        discountLabel.addClassName("total-line-discount");

        var shippingLabel = new Span(formatter("Shipping: $", shippingSignal));
        shippingLabel.addClassName("total-line");

        var taxLabel = new Span(formatter("Tax (8%): $", taxSignal));
        taxLabel.addClassName("total-line");

        var divider = new Div();
        divider.addClassName("totals-divider");

        var totalLabel = new Span(formatter("Total: $", totalSignal));
        totalLabel.addClassName("total-grand");

        totalsBox.add(summaryTitle, subtotalLabel, discountLabel, shippingLabel,
                taxLabel, divider, totalLabel);

        // Info box
        var infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "💡 This use case demonstrates computed signals with multiple levels of calculation. "
                        + "The subtotal is computed from cart items, the discount is computed from the subtotal and discount code, "
                        + "the tax is computed from (subtotal - discount), and the final total combines all values. "
                        + "All calculations happen reactively as you modify any input."));

        add(title, description, productsTitle, productsContainer, cartHeader,
                cartItemsContainer, optionsLayout, totalsBox, infoBox);
    }

    private HorizontalLayout createProductRow(Product product,
            ListSignal<CartItem> cartItemsSignal) {
        var row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.addClassName("product-row");

        var nameLabel = new Span(
                format(product.name() + " - $", product.price()));
        nameLabel.addClassName("row-name");

        var addButton = new Button("Add",
                e -> addToCart(product, cartItemsSignal));
        addButton.addThemeName("primary");
        addButton.addThemeName("small");

        row.add(nameLabel, addButton);
        return row;
    }

    private HorizontalLayout createCartItemRow(ValueSignal<CartItem> itemSignal,
            ListSignal<CartItem> cartItemsSignal) {
        var row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.addClassName("cart-item-row");

        var nameLabel = new Span(
                itemSignal.map(item -> format(item.product().name() + " - $",
                        item.product().price())));
        nameLabel.addClassName("row-name");

        var quantityField = new IntegerField();
        quantityField.setMin(1);
        quantityField.setMax(99);
        quantityField.setWidth("120px");
        quantityField.setStepButtonsVisible(true);

        // Two-way binding for quantity using map (read) + updater (write)
        quantityField.bindValue(itemSignal.map(CartItem::quantity),
                itemSignal.updater(CartItem::withQuantity));

        // Handle removal when quantity drops below 1
        quantityField.addValueChangeListener(e -> {
            var value = e.getValue();
            if (value == null || value < 1) {
                cartItemsSignal.remove(itemSignal);
            }
        });

        var itemTotalLabel = new Span(
                formatter("$", itemSignal.map(CartItem::totalPrice)));

        itemTotalLabel.setWidth("100px");
        itemTotalLabel.addClassName("item-total");

        var removeButton = new Button("Remove", e -> {
            cartItemsSignal.remove(itemSignal);
        });
        removeButton.addThemeName("error");
        removeButton.addThemeName("small");

        row.add(nameLabel, quantityField, itemTotalLabel, removeButton);
        return row;
    }

    private void addToCart(Product product,
            ListSignal<CartItem> cartItemsSignal) {
        var existingItem = cartItemsSignal.peek().stream().filter(
                signal -> signal.peek().product().id().equals(product.id()))
                .findFirst();
        existingItem.ifPresentOrElse(
                existing -> existing
                        .update(item -> item.withQuantity(item.quantity() + 1)),
                () -> cartItemsSignal.insertLast(new CartItem(product, 1)));
    }

    private @Nullable DiscountCode validateDiscountCode(String code) {
        // Stub implementation
        return switch (code.toUpperCase()) {
        case "SAVE10" -> new DiscountCode("SAVE10", new BigDecimal("10"));
        case "SAVE20" -> new DiscountCode("SAVE20", new BigDecimal("20"));
        default -> null;
        };
    }

    private BigDecimal getShippingCost(ShippingOption option) {
        return switch (option) {
        case STANDARD -> new BigDecimal("5.99");
        case EXPRESS -> new BigDecimal("12.99");
        case OVERNIGHT -> new BigDecimal("24.99");
        };
    }

    private static Signal<String> formatter(String prefix,
            Signal<BigDecimal> signal) {
        return () -> format(prefix, signal.get());
    }

    private static String format(String prefix, BigDecimal amount) {
        return prefix + amount.setScale(2, RoundingMode.HALF_UP);
    }
}
