package com.example.uc4;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.acme.AppWindow;
import com.example.acme.DemoRig;
import com.example.acme.Investigation;
import com.example.acme.Telemetry;
import com.example.views.MainLayout;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
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
import com.vaadin.flow.shared.Registration;

/**
 * UC4 — dispatching a shipment at Acme sometimes takes a second and sometimes
 * fails, and the request says {@code outcome=success} either way: how do you
 * follow that one click from the browser event down to the carrier call?
 * <p>
 * The view opens with the story: an {@link AppWindow} showing the shipping
 * desk, whose "Dispatch shipment" handler reserves stock in the product table
 * (one query per line), books a carrier through {@link CarrierService a backend
 * service} — its latency and its refusal in the {@link DemoRig} — and prints a
 * label. The carrier's default latency is deliberately <em>under</em> the kit's
 * 1 s UX budget, so no threshold is crossed, nothing is retained as a slow
 * interaction, and the only thing that can say where the time went is the
 * trail. The first dispatch reveals the {@link Investigation}, which is live:
 * the UI polls, because the request's own span cannot be in the response it
 * wraps.
 * <p>
 * Its steps: <b>2)</b> the trail itself — every span of the interaction nested
 * by parent on a shared timeline, the kit's request and RPC spans, the
 * application's own dispatch, warehouse and carrier spans, and the kit's
 * {@code vaadin.db.query} span per statement, all under one trace id, plus the
 * earlier dispatches the way a trace UI would list them; <b>3)</b> where the
 * time went and where it failed — the spans ranked by the time they spent on
 * their own rather than waiting for something they called, next to what the
 * meters know about the same interaction, which is that it succeeded; <b>4)</b>
 * where the trail begins and ends — it starts on the server rather than at the
 * click, its root span calls this screen by its class name rather than its
 * route, it carries no session, and the application has to name its own hops
 * (see {@code API-GAPS.md} #3, #14 and #15).
 * <p>
 * Nothing here reads a tracing backend: {@link InteractionTrail} is a
 * {@code SpanReporter}, the same SPI Zipkin, Tempo and OTLP exporters
 * implement, pointed at this page instead of out of the process.
 */
@Route(value = InteractionTraceView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Interaction tracing")
@Menu(order = 4, title = "UC4 — Interaction tracing")
public class InteractionTraceView extends VerticalLayout {

    /**
     * The route template, which is also the {@code route} attribute the kit
     * puts on this interaction's spans. Named after the Acme screen; the
     * {@code uc4} alias keeps the numbered URL working without appearing in the
     * telemetry.
     */
    static final String ROUTE = "shipping";

    /**
     * The carrier's default answer time. Under the kit's 1 s UX budget on
     * purpose: a dispatch that crossed it would be retained as a slow
     * interaction and UC1's and UC6's readouts would already name it. This one
     * is merely *noticeable*, which is the case only a trail can explain.
     */
    static final int DEFAULT_CARRIER_LATENCY_MS = 900;

    /** The application's own span for the whole business operation. */
    static final String DISPATCH = "acme.shipment.dispatch";

    /** The application's own span for the label printer. */
    static final String LABEL_PRINT = "acme.label.print";

    static final String CARRIER_FREIGHT = "Acme Freight";
    static final String CARRIER_OVERNITE = "OverNite Express";
    static final String CARRIER_RAIL = "Rail & Road";

    /** The shipment's lines, which are also its stock lookups. */
    static final List<String> LINES = List.of(
            "Stainless steel hex bolt M8 × 40", "Brass flat washer M6",
            "Galvanized hex nut M10");

    private static final Logger log = LoggerFactory
            .getLogger(InteractionTraceView.class);

    private static final String ERRORS = "vaadin.errors";
    private static final String SERVER_RPC = "vaadin.rpc.duration";
    private static final String TAG_ROUTE = "route";
    private static final int POLL_MILLIS = 2000;

    private final transient MeterRegistry registry;
    private final transient ObservationRegistry observations;
    private final transient Tracer tracer;
    private final transient InteractionTrail trail;
    private final transient WarehouseService warehouse;
    private final transient CarrierService carriers;

    private final Investigation investigation = new Investigation(
            "That is one interaction, and here is all of it — browser event, "
                    + "request, RPC, the desk's own steps, the carrier, every "
                    + "statement — under one trace id. The readout refreshes "
                    + "as you work and every " + (POLL_MILLIS / 1000)
                    + " s on its own, because the request's own span cannot "
                    + "be in the response it wraps.");
    private final Div trailSummary = new Div();
    private final TrailTable trailTable = new TrailTable();
    private final Table recentTrails = new Table();
    private final Div timing = new Div();

    private final Select<String> carrier = new Select<>();
    private final IntegerField carrierLatency = new IntegerField(
            "Carrier API latency (ms)");
    private final Checkbox carrierRefuses = new Checkbox(
            "Carrier refuses the booking");
    private final Span status = new Span();

    private int dispatched;
    private @Nullable String followedTraceId;
    private @Nullable String correlationId;
    private @Nullable Registration pollRegistration;

    /**
     * @param registry
     *            the meter registry, for what the meters know about the very
     *            interaction the trail explains
     * @param observations
     *            the observation registry the kit already records into, so the
     *            desk's own spans nest inside the kit's
     * @param tracer
     *            the tracer, read for the trace id of the interaction being
     *            handled
     * @param trail
     *            the span reporter that keeps the followed interactions
     * @param warehouse
     *            the stock reservation hop, which visits the datastore
     * @param carriers
     *            the carrier booking hop, the backend service at the far end
     */
    public InteractionTraceView(MeterRegistry registry,
            ObservationRegistry observations, Tracer tracer,
            InteractionTrail trail, WarehouseService warehouse,
            CarrierService carriers) {
        this.registry = registry;
        this.observations = observations;
        this.tracer = tracer;
        this.trail = trail;
        this.warehouse = warehouse;
        this.carriers = carriers;

        add(new H1("UC4 — Where did this one dispatch spend its time?"));
        add(new Paragraph(
                "Acme's shipping clerks say dispatching a shipment is "
                        + "sometimes instant and sometimes takes about a "
                        + "second, and that when it goes wrong the desk only "
                        + "says the carrier refused. Neither shows up as a "
                        + "failure or as a slow interaction: the request "
                        + "returns successfully, under the budget. Dispatch "
                        + "one below, then follow that single click through "
                        + "the stack."));

        add(new H3("1 — Dispatch a shipment"));
        add(new Paragraph(
                "Pick a carrier and dispatch. The desk reserves the stock for "
                        + "each line, books the carrier, and prints the "
                        + "label — three steps, one of which you will feel."));

        add(buildShippingDesk());
        add(buildDemoRig());
        add(buildInvestigation());

        investigation.refreshNow();
    }

    // ---------- the Acme shipping desk and its demo rig ----------

    private AppWindow buildShippingDesk() {
        TextField consignee = new TextField("Consignee");
        consignee.setValue("Root & Branch Garden Centers");
        consignee.setReadOnly(true);
        consignee.setWidth("22em");

        Table lines = new Table();
        lines.setId("shipment-lines");
        lines.addClassNames("order-lines", "order-lines-numeric");
        lines.setWidthFull();
        lines.addHeaderRow("Item", "Qty");
        lines.addRow(LINES.get(0), "200");
        lines.addRow(LINES.get(1), "500");
        lines.addRow(LINES.get(2), "200");

        carrier.setLabel("Carrier");
        carrier.setItems(CARRIER_FREIGHT, CARRIER_OVERNITE, CARRIER_RAIL);
        carrier.setValue(CARRIER_FREIGHT);
        carrier.setWidth("14em");
        carrier.setId("carrier");

        Button dispatch = new Button("Dispatch shipment",
                event -> dispatchShipment());
        dispatch.addThemeVariants(ButtonVariant.PRIMARY);

        status.setId("shipment-status");
        status.addClassName("catalog-summary");
        status.setText("Not dispatched");

        return new AppWindow("Acme Supply — Shipping Desk", ROUTE, consignee,
                lines,
                new HorizontalLayout(Alignment.END, carrier, dispatch, status));
    }

    /** The knobs that fake the carrier's answer time and its refusals. */
    private DemoRig buildDemoRig() {
        carrierLatency.setValue(DEFAULT_CARRIER_LATENCY_MS);
        carrierLatency.setWidth("14em");
        carrierLatency.setStepButtonsVisible(true);
        carrierLatency.setMin(0);
        carrierLatency.setMax(5_000);
        carrierLatency.setId("carrier-latency");

        carrierRefuses.setId("carrier-refuses");

        DemoRig rig = new DemoRig(new HorizontalLayout(Alignment.END,
                carrierLatency, carrierRefuses));
        rig.setId("simulation-rig");
        return rig;
    }

    /**
     * One dispatch, as one trail.
     * <p>
     * The application's own span wraps the whole business operation, and the
     * first thing inside it is the trace id: the kit's request and RPC
     * observations are already current on this thread, so the id read here is
     * the id of the interaction as a whole, and telling
     * {@link InteractionTrail} to follow it before the work starts is what
     * makes the spans below reachable — a span that finished before the
     * reporter was told to keep this trace was already dropped.
     * <p>
     * The carrier's refusal is allowed out of the observation, so the carrier
     * and dispatch spans record it, and is handled here, because a shipping
     * desk that crashed on a full lane would be a worse application. That is
     * exactly why the interaction looks successful to every meter: as far as
     * Flow is concerned, nothing failed.
     */
    private void dispatchShipment() {
        String chosen = carrier.getValue();
        String shipment = "SHP-%05d".formatted(4_470 + dispatched + 1);
        investigation.reveal();
        try {
            Observation.createNotStarted(DISPATCH, observations)
                    .contextualName(DISPATCH)
                    .lowCardinalityKeyValue("acme.carrier", chosen)
                    .highCardinalityKeyValue("acme.shipment", shipment)
                    .observe(() -> {
                        follow();
                        log.info("Dispatching {} with {}", shipment, chosen);
                        WarehouseService.Reservation reserved = warehouse
                                .reserve(LINES);
                        String booking = carriers.book(chosen,
                                carrierLatency.getValue(),
                                carrierRefuses.getValue());
                        printLabel();
                        dispatched++;
                        String done = ("%s dispatched with %s, booking %s "
                                + "(%d lines reserved)").formatted(shipment,
                                        chosen, booking, reserved.lines());
                        status.setText(done);
                        Notification.show(done);
                    });
        } catch (CarrierService.CarrierRefusedException refused) {
            // What the clerk sees. The trail is the only place that says the
            // carrier hop is where this came from.
            status.setText(shipment + " not dispatched — carrier refused");
            Notification notification = Notification
                    .show("The carrier refused the booking. Try another "
                            + "carrier.");
            notification.addThemeVariants(NotificationVariant.WARNING);
        }
    }

    /**
     * Records which trace this interaction belongs to, and the correlation id
     * the log lines written inside it carry. Both come from the same place: the
     * tracer's current span, which is the dispatch span this runs inside.
     */
    private void follow() {
        Optional<String> traceId = Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().traceId());
        followedTraceId = traceId.orElse(null);
        traceId.ifPresent(trail::follow);
        // What Brave put in the SLF4J MDC, which is what
        // logging.pattern.correlation interpolates into every log line.
        correlationId = MDC.get("traceId");
    }

    /** The third hop: fast, and on the trail anyway so the contrast shows. */
    private void printLabel() {
        Observation.createNotStarted(LABEL_PRINT, observations)
                .contextualName(LABEL_PRINT).observe(() -> sleep(15));
    }

    // ---------- the investigation, revealed by the first dispatch ----------

    private Investigation buildInvestigation() {
        investigation.setId("investigation");
        investigation.onRefresh(this::refreshReadout);

        trailSummary.setId("trail-summary");
        trailTable.setId("trail-table");
        recentTrails.setId("recent-trails");
        recentTrails.addClassName("meter-table");
        recentTrails.setWidthFull();
        recentTrails.addHeaderRow("Trace", "Carrier", "Outcome", "Wall time",
                "Spans");
        Paragraph recentLead = new Paragraph(
                "The dispatches followed so far, the way a trace UI's search "
                        + "results list them. A real exporter keeps every "
                        + "trace and lets the backend do the filtering; this "
                        + "one keeps only the interactions the desk asked it "
                        + "to follow, so the page never accumulates traffic "
                        + "it is not explaining.");
        investigation.step("2 — The trail of one dispatch", true,
                new Paragraph(
                        "One click, one trace id, and every span that carried "
                                + "it: Spring's HTTP span for the POST at the "
                                + "top, the kit's request and RPC spans "
                                + "inside it, then the desk's own dispatch, "
                                + "warehouse and carrier steps, and one "
                                + "statement span per stock lookup, because "
                                + "the kit's database feature is on. "
                                + "Indentation is nesting, and the bar is "
                                + "when the span ran within the trail."),
                trailSummary, trailTable, recentLead, recentTrails);

        timing.setId("trail-timing");
        timing.setWidthFull();
        investigation.step("3 — Where the time went, and where it failed",
                false,
                new Paragraph(
                        "A span's duration includes everything it waited for, "
                                + "so the ranking below uses the time each "
                                + "span spent on its own — its duration less "
                                + "its children's. That is the number that "
                                + "names a culprit."),
                timing);

        investigation.step("4 — Where the trail begins, and where it ends",
                false, buildLimits());

        return investigation;
    }

    /** Step 4: what the trail cannot tell you, and why. */
    private Div buildLimits() {
        Div limits = new Div();
        limits.setId("trail-limits");
        limits.setWidthFull();

        Paragraph root = new Paragraph();
        root.add(new Span("The trail's root is a server span — Spring's HTTP "
                + "observation of the POST that carried the click, with the "
                + "kit's "), Telemetry.chip("vaadin.request"),
                new Span(
                        " span inside it — and not the click itself. The "
                                + "browser sends no "),
                Telemetry.chip("traceparent"),
                new Span(" on the UIDL request: the kit's in-browser "
                        + "collector posts name, tags, value and timestamp "
                        + "and nothing else. So although this application "
                        + "would happily continue a W3C trace it was given "
                        + "("),
                Telemetry.chip("management.tracing.propagation.consume"),
                new Span(" includes W3C), there is no upstream span to "
                        + "descend from. The click, the network and the "
                        + "browser's rendering of the response are therefore "
                        + "outside the trail; UC1 reads them as aggregates "
                        + "instead ("),
                new Anchor(
                        "https://github.com/vaadin/use-cases/blob/main/"
                                + "observability/API-GAPS.md",
                        "API-GAPS.md #3"),
                new Span(")."));

        Paragraph uri = new Paragraph();
        uri.add(new Span("That root span is also where the kit's route "
                + "attribution stops agreeing with itself. Every kit meter "
                + "and span below it is tagged "),
                Telemetry.chip(TAG_ROUTE + "=" + ROUTE), new Span(", but the "),
                Telemetry.chip("uri"),
                new Span(" the kit lifts into Spring's HTTP observation reads "
                        + "this view's class name instead of its route "
                        + "template, so a dashboard grouping "),
                Telemetry.chip("http.server.requests"),
                new Span(" by URI and one grouping the kit's own timers by "
                        + "route disagree about what to call the same screen "
                        + "("),
                new Anchor(
                        "https://github.com/vaadin/use-cases/blob/main/"
                                + "observability/API-GAPS.md",
                        "API-GAPS.md #15"),
                new Span(")."));

        Paragraph session = new Paragraph();
        session.add(new Span("No span carries a session, so \"show me this "
                + "user's interaction\" is not a question the trail answers. "
                + "The kit declares the switch for it — "),
                Telemetry.chip("vaadin.observability.traces-session-id"),
                new Span(" and a "), Telemetry.chip("vaadin.session.id"),
                new Span(" attribute key — but nothing applies either, so "
                        + "turning it on changes nothing ("),
                new Anchor(
                        "https://github.com/vaadin/use-cases/blob/main/"
                                + "observability/API-GAPS.md",
                        "API-GAPS.md #14"),
                new Span(")."));

        Paragraph own = new Paragraph(
                "The kit instruments the framework, not the business: the "
                        + "request, the RPC, navigations, data provider "
                        + "queries and JDBC statements come free, but "
                        + "\"reserve stock\" and \"book the carrier\" are "
                        + "steps only this application knows about. Naming "
                        + "one costs this much, and it nests under whatever "
                        + "the kit already has open:");

        Pre snippet = new Pre("""
                Observation.createNotStarted("acme.carrier.book", observations)
                        .contextualName("acme.carrier.book")
                        .lowCardinalityKeyValue("acme.carrier", carrier)
                        .observe(() -> carrierApi.book(shipment));""");
        snippet.addClassName("payload");
        Div snippetBox = new Div(snippet);
        snippetBox.addClassName("payload-scroller");

        Paragraph export = new Paragraph();
        export.add(new Span("And nothing here reads a tracing backend. "),
                Telemetry.chip("InteractionTrail"), new Span(" is a "),
                Telemetry.chip("SpanReporter"),
                new Span(", the same interface Zipkin, Tempo and the OTLP "
                        + "exporters implement, pointed at this page instead "
                        + "of out of the process: swap the bean and these "
                        + "spans land in a trace UI unchanged. What has to "
                        + "change with it is sampling — Boot keeps one trace "
                        + "in ten by default, and this module sets "),
                Telemetry.chip("management.tracing.sampling.probability=1.0"),
                new Span(" so the dispatch you just made is the one you can "
                        + "read."));

        limits.add(root, uri, session, own, snippetBox, export);
        return limits;
    }

    // ---------- polling: what makes the readout live ----------

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        // The request span of a dispatch finishes after that dispatch's
        // response has been written, so the trail is complete only on a later
        // round trip. setPollInterval and the listener live on the UI, which
        // outlives this view, so both are undone in onDetach.
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
        List<InteractionTrail.Span> spans = followedTraceId == null ? List.of()
                : trail.spans(followedTraceId);
        refreshTrail(spans);
        refreshRecentTrails();
        refreshTiming(spans);
    }

    /** Step 2: the trail itself, with what identifies it. */
    private void refreshTrail(List<InteractionTrail.Span> spans) {
        trailSummary.removeAll();
        trailTable.setTrail(spans);
        if (followedTraceId == null) {
            trailSummary.add(new Paragraph(
                    "No dispatch followed yet — dispatch a shipment above and "
                            + "its trail appears here."));
            return;
        }
        if (spans.isEmpty()) {
            trailSummary.add(new Paragraph(
                    "The interaction has a trace id but no spans arrived. "
                            + "That is what an unsampled trace looks like: "
                            + "the ids exist, the spans are never exported."));
            return;
        }
        Paragraph identity = new Paragraph();
        identity.add(new Span("Trace "), Telemetry.chip(followedTraceId),
                new Span(" — "),
                Telemetry.timing("%d spans".formatted(spans.size())),
                new Span(" over "),
                Telemetry.timing("%.1f ms".formatted(wallTimeMs(spans))),
                new Span(" of wall time."));
        if (correlationId != null) {
            identity.add(new Span(" Every log line the dispatch wrote carries "
                    + "the same id: Brave puts it in the SLF4J MDC as "),
                    Telemetry.chip("traceId=" + correlationId),
                    new Span(", and "),
                    Telemetry.chip("logging.pattern.correlation"),
                    new Span(" reads it into the console, so the trail says "
                            + "where the time went and the log says what the "
                            + "code was doing there."));
        }
        trailSummary.add(identity);
    }

    /** Step 2's tail: the followed dispatches, newest first. */
    private void refreshRecentTrails() {
        List.copyOf(recentTrails.getBody().getRows())
                .forEach(TableRow::removeFromParent);
        List<String> followed = trail.followed();
        if (followed.isEmpty()) {
            TableRow empty = recentTrails.getBody().addRow();
            empty.addDataCell("No dispatches followed yet.").setColspan(5);
            empty.addClassName("order-empty");
            return;
        }
        followed.forEach(traceId -> {
            List<InteractionTrail.Span> spans = trail.spans(traceId);
            TableRow row = recentTrails.getBody().addRow();
            row.addDataCell(Telemetry.chip(shorten(traceId)));
            row.addDataCell(attributeOf(spans, DISPATCH, "acme.carrier"));
            boolean failed = spans.stream()
                    .anyMatch(InteractionTrail.Span::failed);
            row.addDataCell(failed ? "error" : "success");
            if (spans.isEmpty()) {
                row.addDataCell("—");
            } else {
                row.addDataCell(Telemetry
                        .timing("%.1f ms".formatted(wallTimeMs(spans))));
            }
            row.addDataCell(Integer.toString(spans.size()));
            if (failed) {
                row.addClassNames("trail-failed", "v-error");
            }
        });
    }

    /**
     * Step 3: the spans ranked by their own time, the failure if there was one,
     * and what the meters made of the same interaction.
     */
    private void refreshTiming(List<InteractionTrail.Span> spans) {
        timing.removeAll();
        if (spans.isEmpty()) {
            Paragraph empty = new Paragraph(
                    "Once a dispatch has been followed, its spans are ranked "
                            + "here by the time each spent on its own.");
            empty.addClassName("verdict-empty");
            timing.add(empty);
            return;
        }

        Map<String, Double> childTime = spans.stream()
                .filter(span -> span.parentId() != null)
                .collect(Collectors.groupingBy(InteractionTrail.Span::parentId,
                        Collectors.summingDouble(
                                InteractionTrail.Span::durationMs)));
        double total = wallTimeMs(spans);
        List<Ranked> ranked = spans.stream()
                .map(span -> new Ranked(span, Math.max(0,
                        span.durationMs()
                                - childTime.getOrDefault(span.spanId(), 0.0))))
                .sorted(Comparator.comparingDouble(Ranked::selfMs).reversed())
                .toList();

        Table ranking = new Table();
        ranking.setId("trail-ranking");
        ranking.addClassName("meter-table");
        ranking.setWidthFull();
        ranking.addHeaderRow("Span", "Own time", "Share of the trail",
                "Outcome");
        ranked.forEach(entry -> {
            TableRow row = ranking.getBody().addRow();
            row.addDataCell(Telemetry.chip(entry.span().name()));
            row.addDataCell(
                    Telemetry.timing("%.1f ms".formatted(entry.selfMs())));
            row.addDataCell("%.0f %%"
                    .formatted(100 * entry.selfMs() / Math.max(0.001, total)));
            row.addDataCell(entry.span().failed() ? "error" : "success");
            if (entry.span().failed()) {
                row.addClassNames("trail-failed", "v-error");
            }
        });
        Ranked worst = ranked.get(0);
        Paragraph verdict = Telemetry.highlightDurations(
                ("%s spent %.0f ms of the trail's %.0f ms on its own — not "
                        + "waiting for anything it called. That is the hop to "
                        + "look at, and no meter in this application could "
                        + "have named it.")
                        .formatted(worst.span().name(), worst.selfMs(), total));
        timing.add(verdict, ranking);

        origin(spans).ifPresent(culprit -> {
            Span message = new Span(String.valueOf(culprit.error()));
            message.addClassName("trail-error");
            Paragraph failure = new Paragraph();
            failure.add(new Span("The failure entered the trail at "),
                    Telemetry.chip(culprit.name()), new Span(" — "), message,
                    new Span(" — and the spans above it, the RPC and the "
                            + "request, are "),
                    Telemetry.chip("outcome=success"),
                    new Span(", because the desk handled the refusal and, as "
                            + "far as Flow is concerned, nothing failed."));
            timing.add(failure);
        });
        timing.add(meterView());
    }

    /**
     * Where a failure entered the trail: the innermost failed span, i.e. the
     * failed span none of whose children failed. A failure the application lets
     * out of a hop marks that hop and every enclosing one, so the outermost red
     * span is the least informative of them — the question is which hop
     * introduced it.
     */
    private static Optional<InteractionTrail.Span> origin(
            List<InteractionTrail.Span> spans) {
        List<InteractionTrail.Span> failed = spans.stream()
                .filter(InteractionTrail.Span::failed).toList();
        return failed.stream()
                .filter(span -> failed.stream().noneMatch(
                        other -> span.spanId().equals(other.parentId())))
                .findFirst();
    }

    /**
     * What the application's meters made of the same interaction: an error
     * count on this route that a handled failure never reaches, and an RPC
     * timer that knows a click took about a second without knowing which part
     * of it did.
     */
    private Paragraph meterView() {
        double errors = registry.find(ERRORS).tag(TAG_ROUTE, ROUTE).counters()
                .stream().mapToDouble(Counter::count).sum();
        Paragraph meters = new Paragraph();
        meters.add(new Span("Meanwhile "), Telemetry.chip(ERRORS),
                new Span(" on "), Telemetry.chip(TAG_ROUTE + "=" + ROUTE),
                new Span(" stands at "),
                Telemetry.timing("%.0f".formatted(errors)), new Span(" and "),
                Telemetry.chip(SERVER_RPC), new Span(" at "),
                Telemetry.timing(rpcReading()),
                new Span(" — tagged by RPC type and outcome only, so it can "
                        + "say an interaction took that long and never which "
                        + "of its hops did."));
        return meters;
    }

    private String rpcReading() {
        long count = 0;
        double totalMs = 0;
        double maxMs = 0;
        for (Timer timer : registry.find(SERVER_RPC).timers()) {
            count += timer.count();
            totalMs += timer.totalTime(TimeUnit.MILLISECONDS);
            maxMs = Math.max(maxMs, timer.max(TimeUnit.MILLISECONDS));
        }
        return count == 0 ? "no samples yet"
                : "mean %.1f ms, max %.1f ms".formatted(totalMs / count, maxMs);
    }

    /** One span with the time it spent on its own. */
    private record Ranked(InteractionTrail.Span span, double selfMs) {
    }

    /** From the first span's start to the last one's end. */
    private static double wallTimeMs(List<InteractionTrail.Span> spans) {
        Instant start = spans.stream().map(InteractionTrail.Span::start)
                .min(Comparator.naturalOrder()).orElseThrow();
        Instant end = spans.stream().map(InteractionTrail.Span::end)
                .max(Comparator.naturalOrder()).orElseThrow();
        return Duration.between(start, end).toNanos() / 1_000_000.0;
    }

    /** One attribute of one named span of a trail, a dash when absent. */
    private static String attributeOf(List<InteractionTrail.Span> spans,
            String spanName, String attribute) {
        return spans.stream().filter(span -> spanName.equals(span.name()))
                .map(span -> span.tags().getOrDefault(attribute, "—"))
                .findFirst().orElse("—");
    }

    /** A trace id is 32 hex characters; a trace UI shows the first few. */
    private static String shorten(String traceId) {
        return traceId.length() <= 12 ? traceId
                : traceId.substring(0, 12) + "…";
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
