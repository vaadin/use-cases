package com.example.uc9;

import java.time.Duration;

import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — Feedback while a request is slow.
 * <p>
 * Some actions are simply slow and run on the request thread. Three built-in
 * tools keep that from feeling broken: a button that disables itself on click,
 * so an impatient second click cannot place the order twice; the loading
 * indicator, whose delays decide when the user sees that the server is busy;
 * and value change modes, which decide how many requests typing in a field
 * sends.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Feedback on slow requests")
@UseCaseDescription("Disabling buttons, tuning the loading indicator and sending fewer requests while typing")
@Menu(order = 9, title = "UC9 — Slow request feedback")
public class SlowRequestFeedbackView extends VerticalLayout {

    static final Duration ORDER_LATENCY = Duration.ofMillis(1500);
    static final int LAZY_TIMEOUT_MS = 400;

    private final SimulatedLatency latency;

    private final IntegerField firstDelay = new IntegerField(
            "Show the loading indicator after (ms)");
    private final Span ordersPlaced = new Span();
    private final Span requestsSent = new Span();
    private int orders;
    private int requests;

    public SlowRequestFeedbackView(SimulatedLatency latency) {
        this.latency = latency;

        add(new H1("UC9 — Feedback on slow requests"));
        add(new Paragraph("Placing an order takes 1.5 seconds on the server. "
                + "Double-click both buttons and compare the order count."));

        Button protectedOrder = new Button("Place order",
                event -> placeOrder(event.getSource()));
        protectedOrder.addThemeVariants(ButtonVariant.PRIMARY);
        protectedOrder.setDisableOnClick(true);
        Button unprotectedOrder = new Button("Place order (no protection)",
                event -> placeOrder(null));
        add(new HorizontalLayout(protectedOrder, unprotectedOrder),
                ordersPlaced);

        add(new H2("Loading indicator"));
        add(new Paragraph("The thin bar at the top appears once a request "
                + "has taken longer than this. Too short and it flickers on "
                + "every click; too long and a slow click looks dead."));
        firstDelay.setStepButtonsVisible(true);
        firstDelay.setStep(100);
        firstDelay.setMin(0);
        firstDelay.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                indicator().setFirstDelay(event.getValue());
            }
        });
        add(firstDelay);

        add(new H2("Requests while typing"));
        TextField search = new TextField("Search");
        Checkbox lazy = new Checkbox(
                "Wait for a pause in typing (" + LAZY_TIMEOUT_MS + " ms)",
                true);
        lazy.addValueChangeListener(
                event -> applyMode(search, event.getValue()));
        applyMode(search, true);
        search.addValueChangeListener(event -> {
            requests++;
            renderCounters();
        });
        add(new HorizontalLayout(search, lazy), requestsSent);

        renderCounters();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        firstDelay.setValue(indicator().getFirstDelay());
    }

    private LoadingIndicatorConfiguration indicator() {
        return getUI().orElseThrow().getLoadingIndicatorConfiguration();
    }

    private void placeOrder(@Nullable Button reEnable) {
        latency.block(ORDER_LATENCY);
        orders++;
        renderCounters();
        if (reEnable != null) {
            reEnable.setEnabled(true);
        }
    }

    private static void applyMode(TextField field, boolean lazy) {
        if (lazy) {
            field.setValueChangeMode(ValueChangeMode.LAZY);
            field.setValueChangeTimeout(LAZY_TIMEOUT_MS);
        } else {
            field.setValueChangeMode(ValueChangeMode.EAGER);
        }
    }

    private void renderCounters() {
        ordersPlaced.setText("Orders placed: " + orders);
        requestsSent.setText("Search requests sent: " + requests);
    }
}
