package com.example.usecase33;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Order management view. An order list is on the left; the right pane shows
 * details of the currently selected order. When the user picks an order, the
 * details pane plays a brief open animation and (in a real app) would fetch
 * related data — an expensive side-effect that should fire on user-driven
 * selection, but NOT every time the server pushes a status update to the
 * already-selected order.
 * <p>
 * The selection signal uses a custom equality checker {@code (a, b) ->
 * a.id() == b.id()}, so a {@code set(updatedSameOrder)} call is a no-op and the
 * open-animation effect does not re-fire. Status changes still reach the UI
 * because the order list itself is the source of truth and the details pane
 * reads the live entry from it.
 */
@PageTitle("Use Case 33: Order list with stable selection")
@Route(value = "use-case-33", layout = MainLayout.class)
@Menu(order = 33, title = "UC 33: Stable selection")
@AnonymousAllowed
public class UseCase33View extends VerticalLayout {

    final ListSignal<Order> orders = new ListSignal<>();
    final ValueSignal<@Nullable Order> selected = new ValueSignal<@Nullable Order>(
            null,
            (a, b) -> a == null ? b == null : b != null && a.id() == b.id());
    final AtomicInteger detailsOpenAnimations = new AtomicInteger();
    private final ValueSignal<Integer> animationsSignal = new ValueSignal<>(0);

    public UseCase33View() {
        setSpacing(true);
        setPadding(true);

        orders.insertAllLast(
                List.of(new Order(1001, "Acme Corp", "Pending", 199.00),
                        new Order(1002, "Globex Ltd", "Shipped", 1450.00),
                        new Order(1003, "Hooli Inc", "Pending", 79.50),
                        new Order(1004, "Initech", "Cancelled", 0.00),
                        new Order(1005, "Soylent", "Delivered", 3200.00)));

        add(new H2("Use Case 33: Order list with stable selection"),
                new Paragraph(
                        "Click an order to open the details pane. The pane's"
                                + " open-animation effect fires only when the"
                                + " selected order's identity changes — even"
                                + " when the server pushes a status update,"
                                + " the animation does NOT replay because"
                                + " the selection signal's equality checker"
                                + " ignores anything but the order id."));

        HorizontalLayout layout = new HorizontalLayout(buildOrderList(),
                buildDetailsPane());
        layout.setWidthFull();

        add(layout, buildServerPushSection(), buildExplanation());

        // The expensive "open" effect: fires every time the selected order
        // identity changes.
        Signal.effect(this, () -> {
            Order pick = selected.get();
            if (pick != null) {
                animationsSignal.set(detailsOpenAnimations.incrementAndGet());
            }
        });
    }

    private Div buildOrderList() {
        Div panel = new Div();
        panel.getStyle().set("flex", "1").set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");

        H3 header = new H3("Orders");
        header.getStyle().set("margin-top", "0");

        Div rows = new Div();
        rows.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-xs)");
        rows.bindChildren(orders, entry -> {
            Div row = new Div();
            row.getStyle().set("padding", "var(--lumo-space-s)")
                    .set("border-radius", "4px").set("cursor", "pointer")
                    .set("background-color", "var(--lumo-base-color)");
            row.getStyle().bind("border-left", Signal.computed(() -> {
                Order o = entry.get();
                Order sel = selected.get();
                boolean isSelected = sel != null && sel.id() == o.id();
                return isSelected ? "4px solid var(--lumo-primary-color)"
                        : "4px solid transparent";
            }));

            Span line = new Span();
            line.bindText(entry.map(o -> "#" + o.id() + " — " + o.customer()
                    + " · " + o.status() + " · $" + o.total()));
            row.add(line);
            row.addClickListener(e -> selected.set(entry.peek()));
            return row;
        });

        panel.add(header, rows);
        return panel;
    }

    private Div buildDetailsPane() {
        Div panel = new Div();
        panel.getStyle().set("flex", "1").set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");

        H3 header = new H3("Details");
        header.getStyle().set("margin-top", "0");

        Span empty = new Span("(No order selected)");
        empty.bindVisible(Signal.computed(() -> selected.get() == null));
        empty.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Pull live data from the orders list rather than the selection
        // signal — so status changes are visible even though the selection
        // signal didn't fire.
        Span customer = new Span();
        customer.bindText(Signal.computed(() -> {
            Order sel = selected.get();
            return sel == null ? ""
                    : currentOrder(sel.id()).map(Order::customer)
                            .orElse("(deleted)");
        }));
        customer.getStyle().set("font-weight", "bold").set("display", "block");

        Span status = new Span();
        status.bindText(Signal.computed(() -> {
            Order sel = selected.get();
            return sel == null ? ""
                    : "Status: " + currentOrder(sel.id()).map(Order::status)
                            .orElse("?");
        }));
        status.getStyle().set("display", "block");

        Span total = new Span();
        total.bindText(Signal.computed(() -> {
            Order sel = selected.get();
            return sel == null ? ""
                    : "Total: $" + currentOrder(sel.id()).map(Order::total)
                            .orElse(0.0);
        }));
        total.getStyle().set("display", "block");

        Span animationCount = new Span();
        animationCount.bindText(
                animationsSignal.map(n -> "Open animations played: " + n));
        animationCount.getStyle().set("display", "block")
                .set("margin-top", "var(--lumo-space-m)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        panel.add(header, empty, customer, status, total, animationCount);
        return panel;
    }

    private java.util.Optional<Order> currentOrder(long id) {
        return orders.get().stream().map(Signal::get).filter(o -> o.id() == id)
                .findFirst();
    }

    private Div buildServerPushSection() {
        Div section = new Div();
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 header = new H3(
                "Simulate server push: rotate status on the selected order");
        header.getStyle().set("margin-top", "0");

        Button push = new Button("Push status update", e -> {
            Order sel = selected.peek();
            if (sel == null) {
                Notification.show("Select an order first");
                return;
            }
            List<ValueSignal<Order>> all = orders.peek();
            for (ValueSignal<Order> entry : all) {
                Order o = entry.peek();
                if (o.id() == sel.id()) {
                    String next = nextStatus(o.status());
                    Order updated = o.withStatus(next);
                    entry.set(updated);
                    // Also push the refreshed object through the selection
                    // signal — id-equality makes this a no-op for the
                    // selection's listeners (no animation replay).
                    selected.set(updated);
                    return;
                }
            }
        });
        section.add(header, push);
        return section;
    }

    private String nextStatus(String current) {
        return switch (current) {
        case "Pending" -> "Shipped";
        case "Shipped" -> "Delivered";
        case "Delivered" -> "Pending";
        default -> "Pending";
        };
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Why a custom equality checker?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "The selection signal cares about identity, not contents."
                        + " An equality checker keyed on Order.id() means"
                        + " calling selected.set(refreshedSameOrder) is a"
                        + " no-op, so subscribers like the open-animation"
                        + " effect don't fire spuriously when the server"
                        + " merely refreshes the order's metadata. Live"
                        + " status, total and customer fields still update"
                        + " because the details pane reads them from the"
                        + " orders list signal, which is the source of"
                        + " truth.");

        box.add(title, p);
        return box;
    }
}
