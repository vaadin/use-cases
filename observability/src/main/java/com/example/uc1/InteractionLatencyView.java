package com.example.uc1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.acme.AppWindow;
import com.example.acme.DemoRig;
import com.example.acme.InsightCard;
import com.example.acme.Insights;
import com.example.acme.Investigation;
import com.example.acme.MeterTable;
import com.example.acme.Telemetry;
import com.example.views.MainLayout;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;
import com.vaadin.observability.spring.boot.VaadinObservabilityEndpoint;

/**
 * UC1 — working an invoice at Acme feels sluggish: which action, and where
 * does the time go?
 * <p>
 * The view opens with the story: an {@link AppWindow} showing the invoicing
 * desk, with three actions of different server cost — saving a draft costs
 * nothing, applying the customer's discounts runs a pricing lookup, issuing the
 * invoice calls the tax service (its latency in the {@link DemoRig}, above the
 * kit's UX budget by default). The first action reveals the
 * {@link Investigation}, which is live: the UI polls so the browser-collected
 * samples, which the collector only flushes every few seconds, show up without
 * another click.
 * <p>
 * Its steps: <b>2)</b> what the framework times — the three segments of a
 * click: the server's {@code vaadin.request.duration} and
 * {@code vaadin.rpc.duration}, tagged only by type and outcome (so they say
 * <em>something</em> took over a second, not what); the browser's own round
 * trip, {@code vaadin.client.request.duration}, from which the server's figure
 * is subtracted to show the network's share; and the time the browser spent
 * applying the response, {@code vaadin.client.render.duration}; plus the
 * browser's navigation and paint signals for page-load quality; <b>3)</b> the
 * kit's verdict — the insights endpoint's {@code slow-user-interaction}
 * findings for this route, naming the component and the event that crossed the
 * budget; <b>4)</b> per action — the business-level timer the application
 * records itself, because meter tags are cardinality-bounded on purpose and a
 * business action name is the application's own to record (see
 * {@code API-GAPS.md} #8).
 * <p>
 * The two browser-side interaction timers are tagged by route, so the rows read
 * them for {@code route=invoices} only. The render timer exists only when
 * Flow's {@code requestTiming} setting is on — the default outside production
 * mode, and {@code vaadin.requestTiming=true} in production — so its row says
 * so when the round trips are there and the render figures are not.
 */
@Route(value = InteractionLatencyView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Interaction latency")
@Menu(order = 1, title = "UC1 — Interaction latency")
public class InteractionLatencyView extends VerticalLayout {

    /**
     * The route template, which is also the {@code route} evidence on the kit's
     * interaction insights. Named after the Acme screen; the {@code uc1} alias
     * keeps the numbered URL working without appearing in the telemetry.
     */
    static final String ROUTE = "invoices";

    /**
     * Above the kit's UX budget (1 s), so issuing an invoice is retained as a
     * slow interaction on the first try.
     */
    static final int DEFAULT_TAX_DELAY_MS = 1_200;

    /** The pricing lookup behind "Apply discounts": noticeable, under budget. */
    static final int PRICING_DELAY_MS = 150;

    /**
     * The application's own per-action timer. The kit's {@code vaadin.rpc
     * .duration} is tagged by RPC type and outcome only, deliberately bounding
     * cardinality, so "which button" is the application's to record.
     */
    static final String ACTION_TIMER = "acme.invoice.action";
    static final String TAG_ACTION = "action";
    static final String ACTION_SAVE = "save-draft";
    static final String ACTION_DISCOUNTS = "apply-discounts";
    static final String ACTION_ISSUE = "issue";

    private static final String INSIGHTS_SECTION = "observability";
    private static final String SERVER_REQUEST = "vaadin.request.duration";
    private static final String SERVER_RPC = "vaadin.rpc.duration";
    private static final String CLIENT_REQUEST = "vaadin.client.request.duration";
    private static final String CLIENT_RENDER = "vaadin.client.render.duration";
    private static final String CLIENT_NAVIGATION = "vaadin.client.navigation.duration";
    private static final String CLIENT_LCP = "vaadin.client.web_vitals.lcp";
    private static final String CLIENT_FCP = "vaadin.client.web_vitals.fcp";
    private static final String TAG_ROUTE = "route";
    /** The server request timer's type tag; {@code uidl} is one interaction. */
    private static final String TAG_REQUEST_TYPE = "vaadin.request.type";
    private static final String REQUEST_TYPE_UIDL = "uidl";
    private static final int POLL_MILLIS = 2000;

    private final transient MeterRegistry registry;
    private final transient VaadinObservabilityEndpoint endpoint;
    private final Investigation investigation = new Investigation(
            "Which of those felt slow? Here is where each one's time went. "
                    + "The readout is live: it refreshes as you work and every "
                    + (POLL_MILLIS / 1000) + " s on its own.");
    private final MeterTable frameworkTimers = new MeterTable("Samples");
    private final Div wireShare = new Div();
    private final Div verdict = new Div();
    private final MeterTable actionTimers = new MeterTable("Clicks");
    private final IntegerField taxDelay = new IntegerField(
            "Tax service latency (ms)");
    private final Span status = new Span();
    private int issued;
    private @Nullable Registration pollRegistration;

    public InteractionLatencyView(MeterRegistry registry,
            VaadinObservabilityEndpoint endpoint) {
        this.registry = registry;
        this.endpoint = endpoint;

        add(new H1("UC1 — Which step of invoicing is slow, and where does "
                + "the time go?"));
        add(new Paragraph(
                "Acme's billing clerks say working an invoice feels sluggish, "
                        + "but not every step: saving is fine, issuing takes "
                        + "ages. Work one below, then follow the time from the "
                        + "framework's timers to the button that spent it."));

        add(new H3("1 — Work an invoice"));
        add(new Paragraph(
                "Save the draft, apply the customer's discounts, then issue "
                        + "the invoice. Notice which one keeps you waiting."));

        add(buildInvoicingDesk());
        add(buildDemoRig());
        add(buildInvestigation());

        investigation.refreshNow();
    }

    // ---------- the Acme invoicing desk and its demo rig ----------

    private AppWindow buildInvoicingDesk() {
        TextField customer = new TextField("Customer");
        customer.setValue("Root & Branch Garden Centers");
        customer.setReadOnly(true);
        customer.setWidth("22em");

        Table lines = new Table();
        lines.addClassNames("order-lines", "order-lines-numeric");
        lines.setWidthFull();
        lines.addHeaderRow("Item", "Amount");
        lines.addRow("Stainless steel hex bolt M8 × 40 × 200", "€ 96.00");
        lines.addRow("Brass flat washer M6 × 500", "€ 45.00");
        lines.addRow("Galvanized hex nut M10 × 200", "€ 38.00");

        status.addClassName("catalog-summary");
        status.setText("Draft — not saved");
        status.setId("invoice-status");

        Button save = action("Save draft", ACTION_SAVE, () -> {
            /* no server-side work */ }, "Draft saved");
        Button discounts = action("Apply discounts", ACTION_DISCOUNTS,
                () -> sleep(PRICING_DELAY_MS), "Discounts applied: € 179.00 "
                        + "→ € 161.10");
        Button issue = action("Issue invoice", ACTION_ISSUE,
                () -> sleep(taxDelay.getValue()),
                () -> "Invoice INV-%05d issued".formatted(24_310 + ++issued));
        issue.addThemeVariants(ButtonVariant.PRIMARY);

        return new AppWindow("Acme Supply — Invoicing", ROUTE, customer, lines,
                new HorizontalLayout(Alignment.CENTER, save, discounts, issue,
                        status));
    }

    private Button action(String label, String name, Runnable serverWork,
            String done) {
        return action(label, name, serverWork, () -> done);
    }

    /**
     * One action of the desk. The server work is timed under the application's
     * own {@link #ACTION_TIMER}, tagged by action name, and the first action
     * reveals the investigation — before the work, so the readout is in the
     * same response even though the kit records the RPC after the listener.
     */
    private Button action(String label, String name, Runnable serverWork,
            java.util.function.Supplier<String> done) {
        return new Button(label, event -> {
            investigation.reveal();
            Timer.Sample sample = Timer.start(registry);
            try {
                serverWork.run();
            } finally {
                sample.stop(registry.timer(ACTION_TIMER, TAG_ACTION, name));
            }
            String message = done.get();
            status.setText(message);
            Notification.show(message);
        });
    }

    /** The knob that fakes the slow tax service behind "Issue invoice". */
    private DemoRig buildDemoRig() {
        taxDelay.setValue(DEFAULT_TAX_DELAY_MS);
        taxDelay.setWidth("14em");
        taxDelay.setStepButtonsVisible(true);
        taxDelay.setMin(0);
        taxDelay.setMax(5_000);
        taxDelay.setId("tax-delay");

        DemoRig rig = new DemoRig(taxDelay);
        rig.setId("simulation-rig");
        return rig;
    }

    // ---------- the investigation, revealed by the first action ----------

    private Investigation buildInvestigation() {
        investigation.setId("investigation");
        investigation.onRefresh(this::refreshReadout);

        frameworkTimers.setId("framework-timers");
        wireShare.setId("wire-share");
        investigation.step("2 — What the framework times", true,
                new Paragraph(
                        "A click has three segments, and the kit times each "
                                + "from where it can be seen. The server times "
                                + "its handling of the request and of the RPC "
                                + "inside it. The browser times the same round "
                                + "trip from its end, so the difference is the "
                                + "network, and then how long it took to apply "
                                + "the response to the page. All of these are "
                                + "tagged by type, outcome or route only: they "
                                + "say something on this page took over a "
                                + "second, not which button."),
                frameworkTimers, wireShare);

        verdict.setId("verdict");
        verdict.setWidthFull();
        investigation.step("3 — The kit's verdict", false, new Paragraph(
                "The insights endpoint keeps the interactions that exceeded "
                        + "the UX budget, with their route, component and "
                        + "event — so \"a click on a Button on the invoices "
                        + "page\" is a finding, not a guess."),
                verdict);

        actionTimers.setId("action-timers");
        Paragraph actionsLead = new Paragraph();
        actionsLead.add(new Span(
                "The kit stops at the component and the event: a business "
                        + "action name would make meter tags unbounded, so it "
                        + "is the application's to record. This desk times "
                        + "each action under "),
                Telemetry.chip(ACTION_TIMER), new Span(" tagged by "),
                Telemetry.chip(TAG_ACTION), new Span(" — one line of code, and "
                        + "the dashboard can group by it ("),
                new Anchor("https://github.com/vaadin/use-cases/blob/main/"
                        + "observability/API-GAPS.md", "API-GAPS.md #8"),
                new Span(")."));
        investigation.step("4 — Per action, the app's own timer", false,
                actionsLead, actionTimers);

        return investigation;
    }

    // ---------- polling: what makes the readout live ----------

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        // setPollInterval and the listener live on the UI, which outlives this
        // view, so both are undone in onDetach — otherwise the UI would keep
        // polling a detached view (and pin it in memory).
        UI ui = event.getUI();
        ui.setPollInterval(POLL_MILLIS);
        pollRegistration = ui.addPollListener(e -> investigation.refreshNow());
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        event.getUI().setPollInterval(-1);
        super.onDetach(event);
    }

    // ---------- the readout ----------

    private void refreshReadout() {
        refreshFrameworkTimers();
        refreshVerdict();
        refreshActionTimers();
    }

    /**
     * Step 2: the three segments of a click, then the page-load signals. The
     * server request row is scoped to UIDL requests so that it and the
     * browser's round trip describe the same population; the two browser
     * interaction timers are scoped to this route, which is the tag the kit
     * puts on them.
     */
    private void refreshFrameworkTimers() {
        Stats serverRequests = stats(registry.find(SERVER_REQUEST)
                .tag(TAG_REQUEST_TYPE, REQUEST_TYPE_UIDL).timers());
        Stats clientRequests = stats(
                registry.find(CLIENT_REQUEST).tag(TAG_ROUTE, ROUTE).timers());
        Stats clientRenders = stats(
                registry.find(CLIENT_RENDER).tag(TAG_ROUTE, ROUTE).timers());
        frameworkTimers.setRows(List.of(
                row(SERVER_REQUEST, TAG_REQUEST_TYPE + "=" + REQUEST_TYPE_UIDL,
                        serverRequests,
                        "Server-side handling of one interaction's request"),
                aggregate(SERVER_RPC,
                        "Server-side handling of one click or keystroke"),
                row(CLIENT_REQUEST, TAG_ROUTE + "=" + ROUTE, clientRequests,
                        "The same round trip as the browser saw it, from "
                                + "sending the request to the last byte of "
                                + "the response"),
                row(CLIENT_RENDER, TAG_ROUTE + "=" + ROUTE, clientRenders,
                        renderReads(clientRequests, clientRenders)),
                aggregate(CLIENT_NAVIGATION,
                        "Client-side navigation, as the browser saw it"),
                aggregate(CLIENT_LCP, "Largest Contentful Paint"),
                aggregate(CLIENT_FCP, "First Contentful Paint")));
        refreshWireShare(serverRequests, clientRequests);
    }

    /**
     * The render row's explanation. The one way it can have no samples while
     * the round trips do is Flow's {@code requestTiming} being off, which is
     * its default in production mode, so that is what the row says then.
     */
    private static String renderReads(Stats clientRequests,
            Stats clientRenders) {
        if (clientRequests.count > 0 && clientRenders.count == 0) {
            return "Applying the response in the browser. No samples: Flow "
                    + "publishes this figure only when its requestTiming "
                    + "setting is on, which it is not in production mode "
                    + "unless vaadin.requestTiming=true is set.";
        }
        return "Applying the response in the browser: the segment neither "
                + "the server nor the network can see";
    }

    /**
     * The network's share of a click, which no single meter records: the
     * browser's mean round trip less the server's mean handling of the same
     * requests.
     */
    private void refreshWireShare(Stats serverRequests, Stats clientRequests) {
        wireShare.removeAll();
        if (clientRequests.count == 0 || serverRequests.count == 0) {
            wireShare.add(new Paragraph(
                    "Once the browser has reported a round trip on this page, "
                            + "the network's share of it appears here."));
            return;
        }
        double wire = Math.max(0,
                clientRequests.mean() - serverRequests.mean());
        wireShare.add(Telemetry.highlightDurations(
                ("Of the browser's mean %.1f ms round trip on this page, the "
                        + "server accounted for %.1f ms. The remaining %.1f ms "
                        + "were the network and the browser's request queue.")
                        .formatted(clientRequests.mean(),
                                serverRequests.mean(), wire)));
    }

    /**
     * Step 3: the {@code slow-user-interaction} findings for this route. Each
     * carries the component, the event and the timing against the budget.
     */
    private void refreshVerdict() {
        verdict.removeAll();
        Map<String, Object> current = endpoint.section(INSIGHTS_SECTION);
        if (!Insights.isActive(current)) {
            Paragraph inactive = new Paragraph(
                    "Nothing was watching: the kit registered no "
                            + "instrumentation, so there is nothing to report "
                            + "rather than nothing to find. In development mode "
                            + "this means no license key was found.");
            inactive.addClassName("verdict-empty");
            verdict.add(inactive);
            return;
        }
        List<Map<String, Object>> findings = Insights.of(current).stream()
                .filter(insight -> "slow-user-interaction"
                        .equals(insight.get("type")))
                .filter(insight -> ROUTE
                        .equals(Insights.evidenceOf(insight).get("route")))
                .toList();
        if (findings.isEmpty()) {
            Paragraph empty = new Paragraph(
                    "No findings yet — the kit keeps an interaction once it "
                            + "exceeds the 1 s UX budget. Issue an invoice "
                            + "above; the tax service is slow enough by "
                            + "default.");
            empty.addClassName("verdict-empty");
            verdict.add(empty);
            return;
        }
        findings.forEach(insight -> {
            Map<String, Object> evidence = Insights.evidenceOf(insight);
            // A slow-interaction finding need not carry all three figures;
            // without them the card has no detail line rather than "null ms".
            String detail = evidence.get("medianDurationMs") != null
                    ? "median %s ms, worst %s ms, budget %s ms".formatted(
                            evidence.get("medianDurationMs"),
                            evidence.get("maxDurationMs"),
                            evidence.get("thresholdMs"))
                    : null;
            verdict.add(new InsightCard(insight, detail,
                    List.of(TAG_ROUTE + "=" + Insights.text(evidence.get("route")),
                            Insights.simpleName(Insights
                                    .text(evidence.get("component"))),
                            "event " + Insights.text(evidence.get("event")),
                            Insights.text(evidence.get("occurrences")) + "×")));
        });
    }

    /** Step 4: the application's own timer, one row per action. */
    private void refreshActionTimers() {
        List<MeterTable.Row> rows = new ArrayList<>();
        registry.find(ACTION_TIMER).timers().stream()
                .sorted(Comparator.comparing(t -> Insights
                        .text(t.getId().getTag(TAG_ACTION))))
                .forEach(t -> rows.add(new MeterTable.Row(ACTION_TIMER,
                        TAG_ACTION + "=" + t.getId().getTag(TAG_ACTION),
                        t.count(),
                        "mean %.0f ms, max %.0f ms".formatted(
                                t.totalTime(TimeUnit.MILLISECONDS)
                                        / Math.max(1, t.count()),
                                t.max(TimeUnit.MILLISECONDS)),
                        describe(t.getId().getTag(TAG_ACTION)))));
        if (rows.isEmpty()) {
            rows.add(new MeterTable.Row(ACTION_TIMER, TAG_ACTION + "=…", 0, "",
                    "One row per action once it has been used"));
        }
        actionTimers.setRows(rows);
    }

    private static String describe(@Nullable String action) {
        if (ACTION_ISSUE.equals(action)) {
            return "Issue invoice: the tax service call, at the demo rig's "
                    + "latency";
        }
        if (ACTION_DISCOUNTS.equals(action)) {
            return "Apply discounts: the pricing lookup";
        }
        if (ACTION_SAVE.equals(action)) {
            return "Save draft: no server work at all";
        }
        return "";
    }

    /** Count, total and max over a set of timers, whatever their tags. */
    private record Stats(long count, double totalMs, double maxMs) {
        double mean() {
            return count == 0 ? 0 : totalMs / count;
        }

        String value() {
            return count == 0 ? ""
                    : "mean %.1f ms, max %.1f ms".formatted(mean(), maxMs);
        }
    }

    private static Stats stats(Collection<Timer> timers) {
        long count = 0;
        double total = 0;
        double max = 0;
        for (Timer t : timers) {
            count += t.count();
            total += t.totalTime(TimeUnit.MILLISECONDS);
            max = Math.max(max, t.max(TimeUnit.MILLISECONDS));
        }
        return new Stats(count, total, max);
    }

    private static MeterTable.Row row(String meter, String tags, Stats stats,
            String reads) {
        return new MeterTable.Row(meter, tags, stats.count, stats.value(),
                reads);
    }

    private MeterTable.Row aggregate(String meter, String reads) {
        return row(meter, "—", stats(registry.find(meter).timers()), reads);
    }

    private static void sleep(@Nullable Integer millis) {
        if (millis == null || millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
