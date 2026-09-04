package com.example.uc6;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.acme.AppWindow;
import com.example.acme.DemoRig;
import com.example.acme.InsightCard;
import com.example.acme.Insights;
import com.example.acme.Investigation;
import com.example.acme.Telemetry;
import com.example.views.MainLayout;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeTable;
import com.vaadin.flow.component.html.NativeTableCell;
import com.vaadin.flow.component.html.NativeTableHeaderCell;
import com.vaadin.flow.component.html.NativeTableRow;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.observability.spring.boot.VaadinObservabilityEndpoint;

/**
 * UC6 — some returns at Acme's returns desk blow up: how do you get from the
 * clerk's "it said something went wrong" to the line of code?
 * <p>
 * The view opens with the story: an {@link AppWindow} showing the returns
 * desk, whose "Process return" handler fails for defective items (a missing
 * inspection template), fails validation for a blank order number, and hangs
 * on bank-transfer refunds (a slow lookup, its latency in the {@link DemoRig}).
 * The {@link Investigation} reveals itself on the first return that fails or
 * hangs, so it appears at the moment the reader has just seen the vague
 * notification a clerk sees.
 * <p>
 * Its steps: <b>2)</b> the error counter, {@code vaadin.errors}, knows
 * <em>that</em> something failed and which exception, even the route and
 * component class — but not which handler, event or line; <b>3)</b> the
 * kit's insights endpoint records every failed or over-budget interaction with
 * the route, component, event and the first application stack frame, grouping
 * repeats into one finding with an occurrence count; <b>4)</b> the same
 * findings as the JSON payload of {@code GET /actuator/vaadin/observability},
 * which is the contract an AI coding agent reads to jump to the offending
 * line.
 * <p>
 * The handler deliberately lets its exception propagate: the kit records a
 * failed interaction only when the invocation actually fails, so catching it
 * would erase the very thing this use case demonstrates. A session
 * {@link ErrorHandler} installed while the view is attached turns the failure
 * into the notification a real application would show. And the investigation
 * is revealed <em>before</em> the work runs — a failing listener never reaches
 * its own last line, and the kit records the failure after the listener body,
 * which is what {@link Investigation#reveal()}'s deferred refresh is for.
 *
 * @see <a href=
 *      "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md">API-GAPS.md</a>
 */
@Route(value = FailureInsightsView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Failure insights")
@Menu(order = 6, title = "UC6 — Failure insights")
public class FailureInsightsView extends VerticalLayout {

    /**
     * The route template, which is also the {@code route} tag on the kit's
     * error counter and the {@code route} evidence of its insights. Named after
     * the Acme screen; the {@code uc6} alias keeps the numbered URL working
     * without appearing in the telemetry.
     */
    static final String ROUTE = "returns";

    /**
     * Above the kit's UX budget (1 s), so a bank-transfer refund is retained
     * as a slow interaction on the first try.
     */
    static final int DEFAULT_BANK_DELAY_MS = 1_500;

    static final String REASON_DAMAGED = "Damaged in transit";
    static final String REASON_WRONG_ITEM = "Wrong item";
    static final String REASON_DEFECTIVE = "Defective";
    static final String REFUND_STORE_CREDIT = "Store credit";
    static final String REFUND_ORIGINAL = "Original payment";
    static final String REFUND_BANK_TRANSFER = "Bank transfer";

    /** The endpoint's selector, i.e. {@code /actuator/vaadin/observability}. */
    private static final String SECTION = "observability";
    private static final String ERRORS = "vaadin.errors";
    private static final String TAG_ROUTE = "route";
    private static final String TAG_EXCEPTION = "exception";
    private static final String TAG_COMPONENT = "component";

    private final transient VaadinObservabilityEndpoint endpoint;
    private final transient ObjectMapper json;
    private final transient MeterRegistry registry;
    private final Investigation investigation = new Investigation(
            "Saw \"something went wrong\"? That is all the clerk saw, too. "
                    + "Here is how you get from that to the line of code — "
                    + "the readout updates with every return.");
    private final NativeTable returnsLog = new NativeTable();
    private final Paragraph errorCounter = new Paragraph();
    private final Div verdict = new Div();
    private final Pre payload = new Pre();
    private final IntegerField bankDelay = new IntegerField(
            "Bank lookup delay (ms)");
    private @Nullable ErrorHandler previousErrorHandler;

    /**
     * @param endpoint
     *            the kit's insights endpoint bean, so this view shows the very
     *            same payload the HTTP endpoint serves
     * @param json
     *            the application's own Jackson mapper, so the payload is
     *            serialized the way the endpoint serializes it
     * @param registry
     *            the meter registry, for the error counter of step 2
     */
    public FailureInsightsView(VaadinObservabilityEndpoint endpoint,
            ObjectMapper json, MeterRegistry registry) {
        this.endpoint = endpoint;
        this.json = json;
        this.registry = registry;

        add(new H1("UC6 — Why do some returns blow up?"));
        add(new Paragraph(
                "Acme's returns desk has a reputation. Clerks say returns for "
                        + "defective items fail with \"something went wrong\", "
                        + "and refunds by bank transfer hang — and that is all "
                        + "they can tell you. Reproduce it below, then follow "
                        + "how the kit turns \"it broke\" into the line of "
                        + "code."));

        add(new H3("1 — Process a few returns"));
        add(new Paragraph(
                "Register a return for a damaged item — it goes through. Then "
                        + "one for a defective item, and one refunded by bank "
                        + "transfer. The clerks are right."));

        add(buildReturnsDesk());
        add(buildDemoRig());
        add(buildInvestigation());

        investigation.refreshNow();
    }

    // ---------- the Acme returns desk and its demo rig ----------

    private AppWindow buildReturnsDesk() {
        TextField orderNumber = new TextField("Order number");
        orderNumber.setValue("AC-10482");
        orderNumber.setWidth("10em");
        orderNumber.setId("order-number");

        Select<String> reason = new Select<>();
        reason.setLabel("Reason");
        reason.setItems(REASON_DAMAGED, REASON_WRONG_ITEM, REASON_DEFECTIVE);
        reason.setValue(REASON_DAMAGED);
        reason.setWidth("12em");
        reason.setId("return-reason");

        Select<String> refund = new Select<>();
        refund.setLabel("Refund");
        refund.setItems(REFUND_STORE_CREDIT, REFUND_ORIGINAL,
                REFUND_BANK_TRANSFER);
        refund.setValue(REFUND_STORE_CREDIT);
        refund.setWidth("12em");
        refund.setId("refund-method");

        NativeTableCell noReturnsCell = new NativeTableCell(
                "No returns processed yet.");
        noReturnsCell.getElement().setAttribute("colspan", "3");
        NativeTableRow noReturns = new NativeTableRow(noReturnsCell);
        noReturns.addClassName("order-empty");

        Button process = new Button("Process return", event -> {
            String order = orderNumber.getValue().trim();
            String why = reason.getValue();
            String how = refund.getValue();

            // Before the work, not after: a failing handler never reaches its
            // own last line, and the kit records the failure after the handler
            // body — the deferred refresh inside reveal() catches it.
            if (order.isEmpty() || REASON_DEFECTIVE.equals(why)
                    || REFUND_BANK_TRANSFER.equals(how)) {
                investigation.reveal();
            } else {
                investigation.refreshSoon();
            }

            if (order.isEmpty()) {
                throw new IllegalArgumentException(
                        "Order number must not be blank");
            }
            if (REASON_DEFECTIVE.equals(why)) {
                throw new IllegalStateException(
                        "Inspection template 'defective' not found");
            }
            if (REFUND_BANK_TRANSFER.equals(how)) {
                lookUpBankAccount();
            }

            noReturns.removeFromParent();
            returnsLog.getBody()
                    .add(new NativeTableRow(new NativeTableCell(order),
                            new NativeTableCell(why),
                            new NativeTableCell(how)));
            Notification.show("Return registered for " + order);
        });
        process.addThemeVariants(ButtonVariant.PRIMARY);

        returnsLog.setId("returns-log");
        returnsLog.addClassName("order-lines");
        returnsLog.setWidthFull();
        NativeTableRow header = returnsLog.getHead().addRow();
        header.add(new NativeTableHeaderCell("Order"));
        header.add(new NativeTableHeaderCell("Reason"));
        header.add(new NativeTableHeaderCell("Refund"));
        returnsLog.getBody().add(noReturns);

        return new AppWindow("Acme Supply — Returns Desk", ROUTE,
                new HorizontalLayout(Alignment.END, orderNumber, reason,
                        refund, process),
                returnsLog);
    }

    /** The knob that fakes the slow bank lookup behind bank-transfer refunds. */
    private DemoRig buildDemoRig() {
        bankDelay.setValue(DEFAULT_BANK_DELAY_MS);
        bankDelay.setWidth("14em");
        bankDelay.setStepButtonsVisible(true);
        bankDelay.setMin(0);
        bankDelay.setMax(5_000);
        bankDelay.setId("bank-delay");

        DemoRig rig = new DemoRig(bankDelay);
        rig.setId("simulation-rig");
        return rig;
    }

    private void lookUpBankAccount() {
        Integer ms = bankDelay.getValue();
        if (ms == null || ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------- the investigation, revealed by the first bad return --------

    private Investigation buildInvestigation() {
        investigation.setId("investigation");
        investigation.onRefresh(this::refreshReadout);

        errorCounter.setId("error-counter");
        investigation.step("2 — The usual suspects know that, not where", true,
                errorCounter);

        verdict.setId("verdict");
        verdict.setWidthFull();
        investigation.step("3 — The kit's verdict", false, new Paragraph(
                "The insights endpoint records every failed or over-budget "
                        + "interaction with its route, component, event and "
                        + "the first application stack frame, and groups "
                        + "repeats — one finding however many clerks hit it."),
                verdict);

        payload.addClassName("payload");
        Div scroller = new Div(payload);
        scroller.addClassName("payload-scroller");
        Paragraph payloadLead = new Paragraph();
        payloadLead.add(new Span("The same findings as JSON from "),
                new Anchor("/actuator/vaadin/observability",
                        "GET /actuator/vaadin/observability"),
                new Span(" — the contract an AI coding agent reads to jump "
                        + "straight to the offending line."));
        investigation.step("4 — The payload an agent reads", false,
                payloadLead, scroller);

        return investigation;
    }

    private void refreshReadout() {
        Map<String, Object> current = endpoint.section(SECTION);
        refreshErrorCounter();
        refreshVerdict(current);
        refreshPayload(current);
    }

    /**
     * Step 2: the counter a dashboard would alert on. It carries the exception
     * type, the route and the component class — enough to know something is
     * wrong on the returns desk, not enough to know which handler or line.
     */
    private void refreshErrorCounter() {
        errorCounter.removeAll();
        Map<String, Double> byException = new LinkedHashMap<>();
        double total = 0;
        for (Counter counter : registry.find(ERRORS).tag(TAG_ROUTE, ROUTE)
                .counters()) {
            String key = Insights.simpleName(
                    Insights.text(counter.getId().getTag(TAG_EXCEPTION)))
                    + " on " + Insights.simpleName(Insights
                            .text(counter.getId().getTag(TAG_COMPONENT)));
            byException.merge(key, counter.count(), Double::sum);
            total += counter.count();
        }
        if (total == 0) {
            errorCounter.add(new Span(
                    "No failures counted on this route yet — process a return "
                            + "for a defective item above."));
            return;
        }
        errorCounter.add(Telemetry.chip(ERRORS), new Span(" — "),
                Telemetry.timing("%.0f failure(s)".formatted(total)),
                new Span(" on " + TAG_ROUTE + "=" + ROUTE + ": "));
        byException.forEach((key, count) -> errorCounter.add(
                Telemetry.chip(key), new Span(" "),
                Telemetry.timing("×%.0f".formatted(count)), new Span("  ")));
        errorCounter.add(new Span(
                "The counter and the server log know an exception happened, "
                        + "and which type — not which handler, which event, "
                        + "or which line."));
    }

    /**
     * Step 3: the {@code user-interaction-error} and
     * {@code slow-user-interaction} findings for this route, each with the
     * application frame the kit attributes it to.
     */
    private void refreshVerdict(@Nullable Map<String, Object> current) {
        verdict.removeAll();
        List<Map<String, Object>> findings = Insights.of(current).stream()
                .filter(insight -> {
                    Object type = insight.get("type");
                    return "user-interaction-error".equals(type)
                            || "slow-user-interaction".equals(type);
                })
                .filter(insight -> ROUTE
                        .equals(Insights.evidenceOf(insight).get("route")))
                .toList();
        if (findings.isEmpty()) {
            Paragraph empty = new Paragraph(
                    "No findings yet — the kit records an interaction once it "
                            + "fails or exceeds the 1 s UX budget. Process a "
                            + "return for a defective item, or a bank-transfer "
                            + "refund.");
            empty.addClassName("verdict-empty");
            verdict.add(empty);
            return;
        }
        findings.forEach(insight -> {
            Map<String, Object> evidence = Insights.evidenceOf(insight);
            Object frame = evidence.get("applicationFrame");
            Object exception = evidence.get("exception");
            List<String> chips = new ArrayList<>(List.of(
                    TAG_ROUTE + "=" + Insights.text(evidence.get("route")),
                    Insights.simpleName(
                            Insights.text(evidence.get("component"))),
                    "event " + Insights.text(evidence.get("event"))));
            if (exception != null) {
                chips.add(Insights.simpleName(exception.toString()));
            }
            chips.add(Insights.text(evidence.get("occurrences")) + "×");
            verdict.add(new InsightCard(insight,
                    frame == null ? null : "at " + frame, chips));
        });
    }

    /** Step 4: the payload verbatim, as the endpoint serves it. */
    private void refreshPayload(@Nullable Map<String, Object> current) {
        if (current == null || current.isEmpty()) {
            payload.setText("// nothing recorded yet");
            return;
        }
        try {
            payload.setText(json.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(current));
        } catch (RuntimeException e) {
            payload.setText("// could not serialize the payload: " + e);
        }
    }

    // ---------- what the clerk sees when it breaks ----------

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        UI ui = event.getUI();

        // Surface failures the way an application would, without swallowing
        // them: the exception has already propagated through Flow (and been
        // recorded by the kit) by the time the handler runs.
        VaadinSession session = ui.getSession();
        if (session != null) {
            previousErrorHandler = session.getErrorHandler();
            session.setErrorHandler(errorEvent -> ui.access(() -> {
                Notification notification = Notification.show(
                        "Something went wrong. The return was not processed.");
                notification.setPosition(Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.ERROR);
            }));
        }
    }

    @Override
    protected void onDetach(DetachEvent event) {
        UI ui = event.getUI();
        VaadinSession session = ui.getSession();
        if (session != null && previousErrorHandler != null) {
            session.setErrorHandler(previousErrorHandler);
            previousErrorHandler = null;
        }
        super.onDetach(event);
    }
}
