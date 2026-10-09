package com.example.uc4;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.example.AsyncState;
import com.example.MissingAPI;
import com.example.backend.Order;
import com.example.backend.OrderService;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Open a detail route immediately and stream the data in.
 * <p>
 * Looking an order up takes over a second. Doing that inside
 * {@code setParameter} would freeze the navigation: the old view stays on
 * screen and the URL does not change until the lookup returns. Instead the
 * route renders at once — the heading already knows the order number — and the
 * details arrive through push. Jumping to another order while one is still
 * loading drops the older answer, and an unknown order number becomes a "not
 * found" message inside the view.
 * <p>
 * The router offers no asynchronous navigation hook, so a missing order can no
 * longer be turned into the application's real not-found page once the lookup
 * has finished (see API-GAPS.md).
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Detail that streams in")
@UseCaseDescription("Navigating at once and loading the route's data in the background")
@Menu(order = 4, title = "UC4 — Detail that streams in")
@StyleSheet("uc4.css")
public class OrderDetailView extends VerticalLayout
        implements HasUrlParameter<Long> {

    static final Duration LOOKUP_LATENCY = Duration.ofMillis(1200);
    static final List<Long> SAMPLE_IDS = List.of(42L, 1_001L, 777_777L,
            9_999_999L);

    private final OrderService orders;
    private final SimulatedLatency latency;

    private final H2 heading = new H2();
    private final ValueSignal<AsyncState<Optional<Order>>> order = new ValueSignal<>(
            AsyncState.loading());
    private final Div details = new Div();
    private final Span notFound = new Span();
    private @Nullable Registration pending;

    public OrderDetailView(OrderService orders, SimulatedLatency latency) {
        this.orders = orders;
        this.latency = latency;
        addClassName("uc4-view");

        add(new H1("UC4 — Detail that streams in"));
        add(new Paragraph("Open an order. The view appears immediately and "
                + "the details follow about a second later. Click quickly "
                + "between orders: only the last one you chose is shown. "
                + "The last link points to an order that does not exist."));

        HorizontalLayout links = new HorizontalLayout();
        SAMPLE_IDS.forEach(id -> links.add(
                new RouterLink("Order #" + id, OrderDetailView.class, id)));

        details.addClassName("order-details");
        add(links, heading, details);
        renderDetails();
    }

    @Override
    public void setParameter(BeforeEvent event,
            @Nullable @OptionalParameter Long id) {
        if (pending != null) {
            pending.remove();
            pending = null;
        }
        if (id == null) {
            heading.setText("No order selected");
            details.setVisible(false);
            return;
        }
        heading.setText("Order #" + id);
        notFound.setText("Order #" + id + " does not exist.");
        details.setVisible(true);
        long orderId = id;
        pending = MissingAPI.load(this, order, () -> latency
                .after(LOOKUP_LATENCY, () -> orders.find(orderId)));
    }

    private void renderDetails() {
        Div skeleton = new Div();
        skeleton.addClassName("skeleton");
        skeleton.bindVisible(order.map(AsyncState::isLoading));

        notFound.addClassName("not-found");
        notFound.bindVisible(order.map(
                state -> state instanceof AsyncState.Loaded<Optional<Order>>(Optional<Order> value)
                        && value.isEmpty()));

        Span failed = new Span();
        failed.addClassName("not-found");
        failed.bindText(order.map(
                state -> state instanceof AsyncState.Failed<Optional<Order>>(Throwable error)
                        ? "Lookup failed: " + error.getMessage()
                        : ""));
        failed.bindVisible(order.map(AsyncState::isFailed));

        Div fields = new Div(field("Customer", Order::customer),
                field("Product", Order::product),
                field("Quantity", found -> String.valueOf(found.quantity())),
                field("Total", found -> "€ " + found.total()),
                field("Ordered", found -> found.ordered().toString()));
        fields.addClassName("order-fields");
        fields.bindVisible(order.map(state -> found(state) != null));

        details.add(skeleton, notFound, failed, fields);
    }

    private Div field(String label, SerializableFunction<Order, String> value) {
        Span caption = new Span(label);
        caption.addClassName("field-label");
        Span text = new Span();
        text.addClassName("field-value");
        text.bindText(order.map(state -> {
            Order found = found(state);
            return found == null ? "" : value.apply(found);
        }));
        return new Div(caption, text);
    }

    private static @Nullable Order found(AsyncState<Optional<Order>> state) {
        return state instanceof AsyncState.Loaded<Optional<Order>>(Optional<Order> value)
                ? value.orElse(null)
                : null;
    }
}
