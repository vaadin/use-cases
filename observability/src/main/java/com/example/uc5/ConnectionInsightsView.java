package com.example.uc5;

import java.util.ArrayList;
import java.util.Collection;
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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableDataCell;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.html.UnorderedList;
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
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.spring.boot.VaadinObservabilityEndpoint;

/**
 * UC5 — Acme's warehouse pickers say the app freezes on the floor, and that
 * the stock chart never comes up: how do you see a problem that never reaches
 * a server log?
 * <p>
 * The view opens with the story: an {@link AppWindow} showing the picking
 * screen the crew works from on tablets. Confirming a pick is the job and goes
 * through; <em>Show stock levels</em> throws in the browser and <em>Sync stock
 * from the API</em> leaves a promise rejected, so both fail where no server
 * ever hears about them; and the {@link DemoRig} takes the connection away the
 * way the loading dock does. The {@link Investigation} reveals itself the first
 * time one of those problems is felt.
 * <p>
 * Its steps: <b>2)</b> the server-side suspects see nothing — a browser error
 * never reaches {@code vaadin.errors}, and an unreachable browser is a session
 * that simply goes quiet; <b>3)</b> the kit's insights endpoint retains the
 * errors themselves, grouped, with the location parsed out of the stack and
 * the offline time a report waited; <b>4)</b> the raw client meters, which are
 * what an outage looks like fleet-wide; <b>5)</b> what the numbers still cannot
 * tell you.
 * <p>
 * All of it is the Observability Kit's. The in-browser collector subscribes to
 * Flow's own {@code window.Vaadin.connectionState} and records
 * {@link MeterNames#CLIENT_CONNECTION} per transition and
 * {@link MeterNames#CLIENT_CONNECTION_DOWNTIME} for the time spent unable to
 * reach the server, both tagged {@link MeterNames#TAG_STATE}. Three properties
 * of those meters are what step 4 exists to make visible:
 * <ul>
 * <li><b>Downtime is per state, not per outage.</b> Flow enters
 * {@code reconnecting} on the first failed request and only reaches
 * {@code connection-lost} once it has given up retrying, so the two answer
 * different questions — a network that hiccuped versus a server the browser has
 * written off — and a short outage that recovers while Flow is still retrying
 * never enters {@code connection-lost} at all. The readout shows both, and
 * their sum, which is the length of the whole outage.</li>
 * <li><b>The clock is the browser's.</b> A transition into an unreachable state
 * cannot be sent while the browser is in it, so the collector buffers into
 * {@code sessionStorage} and flushes on recovery, measuring the outage on the
 * clock that timestamped it.</li>
 * <li><b>The timer under-reports by construction.</b> A tablet that never comes
 * back reports nothing, so the transition <em>count</em> is the honest measure
 * of how often, and the timer only of how long the observed ones lasted.</li>
 * </ul>
 * <p>
 * Two server-side signals sit alongside them: {@link MeterNames#RESYNC}, which
 * counts the messages a client re-sent having had no answer and the full state
 * rebuilds it asked for after losing one — Flow handles both internally, so
 * without the kit they are invisible — and {@link MeterNames#CLIENT_THROTTLED},
 * which matters here because the reports of one outage all arrive in a single
 * flush and can outrun the per-session rate limit.
 * <p>
 * The <em>detail</em> of a browser error is the kit's too, as of its client
 * error insights. {@link MeterNames#CLIENT_ERRORS} still only counts, tagged
 * {@code uncaught} or {@code promise} — a message would be one time series per
 * distinct message — so what identifies an error is retained as an insight
 * beside the failed server interactions UC6 renders, and served from the same
 * endpoint. Step 3 reads the {@code client-error} insights out of that payload,
 * through the injected {@link VaadinObservabilityEndpoint} bean, and gets for
 * free everything an application-side listener had to do without: grouping by
 * route, kind, source and frame with an occurrence count; a location parsed out
 * of the stack line and validated as a location rather than trusted; and
 * {@code maxBufferedMs}, the offline time a report waited before it could be
 * delivered.
 * <p>
 * Two of those fields are gated. A browser error can quote anything the page
 * was working with, and a page can name a function anything, so the message and
 * the function name travel only when
 * {@code vaadin.observability.insights-details} is on — and for a browser error
 * that setting governs <em>collection</em>, not just retention: with it off the
 * browser never gathers them. This module turns it on for UC6, so they are
 * present here; the payload says which case it is rather than leaving a null.
 * <p>
 * <b>Unlike UC6 and UC8, step 3 is not filtered to this route.</b> A browser
 * error on any Acme screen is one nobody is watching, and the route is part of
 * the grouping key — the same broken script on two screens is two findings —
 * which only shows if the other screens' findings are on the page.
 * <p>
 * <b>Why this view does not poll.</b> A poll is a UIDL request, so a polling
 * view probes the connection on every tick: the loading round trip itself is
 * ignored by the collector, but a poll that gets through ends the outage as far
 * as the browser is concerned, and a polling tab therefore reports shorter
 * downtime than a passive one on the same network. The kit's own README says as
 * much. Nothing pushes the other way either — the collector's ingest raises no
 * event an application can subscribe to (gap #5) — so the readout is recomputed
 * from the interactions themselves: every pick and every failure refreshes it,
 * and {@link #connectionRestored()} refreshes it once the browser is back, from
 * the same script that gave the connection back.
 *
 * @see <a href=
 *      "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md">API-GAPS.md</a>
 */
@Route(value = ConnectionInsightsView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Connection & client problems")
@Menu(order = 5, title = "UC5 — Connection & client problems")
public class ConnectionInsightsView extends VerticalLayout {

    /**
     * The route template, which is also the {@code route} tag on the kit's
     * error counter and — once a browser reports from this screen — the
     * {@code route} evidence of its client error insights. Named after the Acme
     * screen; the {@code uc5} alias keeps the numbered URL working without
     * appearing in the telemetry.
     */
    static final String ROUTE = "picking";

    /** How long a simulated outage lasts, in milliseconds. */
    static final int DEFAULT_OUTAGE_MS = 3_000;

    /**
     * Drives Flow's own connection-state store through a state and back, which
     * is what the kit's own integration test does: it is the same store Flow's
     * reconnect logic drives, so it fires exactly the listeners a real outage
     * fires — the browser's "Connection lost" indicator included.
     * <p>
     * The 300 ms head start matters: this script runs while the response to the
     * click is still being applied, and finishing that response sets the store
     * back to {@code connected}. Waiting for the request to end leaves the
     * simulated state standing.
     * <p>
     * On recovery it drains the collector and calls back, so the readout
     * catches up with the outage that has just ended. {@code this} is the
     * view's own element, which is what makes {@code $server} available.
     */
    private static final String SIMULATE = """
            const self = this;
            const state = $0;
            const millis = $1;
            setTimeout(function () {
                const store = window.Vaadin && window.Vaadin.connectionState;
                if (!store) {
                    return;
                }
                store.state = state;
                setTimeout(function () {
                    store.state = 'connected';
                    if (window.__vaadinMicrometer) {
                        window.__vaadinMicrometer.flush();
                    }
                    self.$server.connectionRestored();
                }, millis);
            }, 300);
            """;

    /** Thrown asynchronously, so it reaches window.onerror uncaught. */
    private static final String THROW = """
            setTimeout(function () {
                throw new Error($0);
            }, 0);
            """;

    private static final String REJECT = "Promise.reject(new Error($0));";

    /**
     * Asks the collector to send what it is holding, so the meters move without
     * waiting for its 5 s timer. Still the debug internal UC2 leans on — there
     * is no public drain (gap #12).
     */
    private static final String FLUSH = """
            window.__vaadinMicrometer && window.__vaadinMicrometer.flush();
            """;

    /** The endpoint's selector, i.e. {@code /actuator/vaadin/observability}. */
    private static final String SECTION = "observability";

    /** The insight type this view reads; the payload carries others. */
    private static final String CLIENT_ERROR = "client-error";

    private static final String ERRORS = "vaadin.errors";
    private static final String TAG_ROUTE = "route";

    private static final String PENDING = "Pending";
    private static final String PICKED = "Picked";

    /** One line of the pick list: a catalog product, its bin and how many. */
    record Pick(String item, String bin, int quantity) {
    }

    /**
     * The order on the tablet. Every item is a product from
     * {@link com.example.acme.AcmeCatalog}, so the screen reads like the rest
     * of the Acme application.
     */
    static final List<Pick> PICK_LIST = List.of(
            new Pick("Brass hex bolt M8 × 40", "A-12-3", 120),
            new Pick("Stainless steel flat washer M8 × 25", "B-04-1", 500),
            new Pick("Zinc-plated wood screw M4 × 20", "C-21-7", 250),
            new Pick("Galvanized lag screw M10 × 50", "D-08-2", 80));

    private final transient MeterRegistry registry;
    private final transient VaadinObservabilityEndpoint endpoint;
    private final Investigation investigation = new Investigation(
            "Felt that? The tablet froze, or the chart never came up — and the "
                    + "server log has nothing to say about either. Here is what "
                    + "does; the readout updates as you keep picking.");
    private final Table pickList = new Table();
    private final List<TableDataCell> statuses = new ArrayList<>();
    private final Paragraph serverSide = new Paragraph();
    private final Div verdict = new Div();
    private final Paragraph summary = new Paragraph();
    private final MeterTable meters = new MeterTable("Reports");
    private final IntegerField outage = new IntegerField("Outage length (ms)");
    private int picked;

    /**
     * @param registry
     *            the application's registry, which the kit's binders — the
     *            in-browser collector's included — publish into
     * @param endpoint
     *            the kit's insights endpoint bean, so the errors in step 3 are
     *            the very same records {@code GET /actuator/vaadin/observability}
     *            serves
     */
    public ConnectionInsightsView(MeterRegistry registry,
            VaadinObservabilityEndpoint endpoint) {
        this.registry = registry;
        this.endpoint = endpoint;

        add(new H1("UC5 — Why does the picking app freeze on the floor?"));
        add(new Paragraph(
                "Acme's warehouse crew picks orders from tablets, and by the "
                        + "loading dock the Wi-Fi is bad. The pickers say the "
                        + "app freezes and then comes back, and that the stock "
                        + "chart never comes up. Neither complaint reaches a "
                        + "server log: a browser that cannot reach the server "
                        + "cannot report anything, and a script that fails in a "
                        + "tab nobody is watching fails silently."));

        add(new H3("1 — Work the pick list"));
        add(new Paragraph(
                "Confirm a pick or two — that part works. Then open the stock "
                        + "levels and sync stock from the warehouse API: both "
                        + "fail in the browser, and the server hears nothing. "
                        + "Finally take the connection away with the demo rig, "
                        + "the way the dead zone by the dock does."));

        add(buildPickingScreen());
        add(buildDemoRig());
        add(buildInvestigation());

        investigation.refreshNow();
    }

    // ---------- the Acme picking screen and its demo rig ----------

    private AppWindow buildPickingScreen() {
        TextField order = new TextField("Order");
        order.setValue("AC-10731");
        order.setReadOnly(true);
        order.setWidth("12em");
        order.setId("order-number");

        pickList.setId("pick-list");
        pickList.addClassName("order-lines");
        pickList.setWidthFull();
        pickList.addHeaderRow("Item", "Bin", "Qty", "Status");
        for (Pick pick : PICK_LIST) {
            TableRow row = pickList.addRow();
            row.addDataCell(pick.item());
            row.addDataCell(pick.bin());
            row.addDataCell(Integer.toString(pick.quantity()));
            statuses.add(row.addDataCell(PENDING));
        }

        Button confirm = new Button("Confirm pick", event -> confirmPick());
        confirm.addThemeVariants(ButtonVariant.PRIMARY);
        confirm.setId("confirm-pick");

        // Two ordinary features of the screen that happen to be broken in the
        // browser. Nothing about either failure is server-side, which is the
        // whole point of the story.
        Button stock = new Button("Show stock levels",
                event -> raise(THROW, "Rendering the stock chart failed"));
        stock.setId("show-stock");

        Button sync = new Button("Sync stock from the API", event -> raise(
                REJECT, "GET /api/warehouse/stock failed"));
        sync.setId("sync-stock");

        return new AppWindow("Acme Supply — Warehouse Picking", ROUTE, order,
                pickList, new HorizontalLayout(Alignment.END, confirm, stock,
                        sync));
    }

    /** The knobs that fake the dead zone by the loading dock. */
    private DemoRig buildDemoRig() {
        outage.setValue(DEFAULT_OUTAGE_MS);
        outage.setWidth("14em");
        outage.setStepButtonsVisible(true);
        outage.setMin(100);
        outage.setMax(15_000);
        outage.setId("outage-length");

        Button lose = new Button("Lose the connection",
                event -> simulate(MeterNames.STATE_CONNECTION_LOST,
                        "The tablet has lost the server for %.1f s. Leave the "
                                + "page alone: any request that gets through "
                                + "ends the outage as far as the browser is "
                                + "concerned."));
        lose.addThemeVariants(ButtonVariant.ERROR);
        lose.setId("simulate-loss");

        // The other unreachable state, and the one a short real outage
        // actually spends its time in.
        Button reconnect = new Button("Flaky signal",
                event -> simulate(MeterNames.STATE_RECONNECTING,
                        "Retrying for %.1f s — the state a short outage "
                                + "recovers from without ever being given up "
                                + "on."));
        reconnect.addThemeVariants(ButtonVariant.WARNING);
        reconnect.setId("simulate-reconnecting");

        DemoRig rig = new DemoRig(outage,
                new HorizontalLayout(Alignment.END, lose, reconnect));
        rig.setId("simulation-rig");
        return rig;
    }

    /** Marks the next line picked. The one thing on this screen that works. */
    private void confirmPick() {
        if (picked == statuses.size()) {
            Notification.show("The pick list is done — nothing left to "
                    + "confirm.");
        } else {
            statuses.get(picked).setText(PICKED);
            Notification.show("Picked " + PICK_LIST.get(picked).item());
            picked++;
        }
        // A pick is not a problem, so it never reveals the investigation; it
        // only brings an already-revealed one up to date.
        investigation.refreshSoon();
    }

    /**
     * Takes the connection away and gives it back, revealing the investigation:
     * the freeze is exactly the thing the pickers complain about.
     */
    private void simulate(String state, String message) {
        int millis = outageMillis();
        investigation.reveal();
        getElement().executeJs(SIMULATE, state, millis);
        Notification.show(message.formatted(millis / 1000d));
    }

    private int outageMillis() {
        Integer value = outage.getValue();
        return value == null || value <= 0 ? DEFAULT_OUTAGE_MS : value;
    }

    /**
     * Raises a browser error, then drains the collector so the insight it
     * produced is on screen before the click is over.
     * <p>
     * The drain has to be a <em>second</em> round trip. The error is thrown
     * from a timeout, so it has not happened yet when this script returns; by
     * the time the flush script arrives from the server it has, and the
     * {@code recordSamples} that flush queues is handled before this second
     * call's own return value — the ordering UC2's flush button relies on. If
     * it ever does not hold, the next pick refreshes the readout anyway.
     */
    private void raise(String script, String message) {
        investigation.reveal();
        getElement().executeJs(script, message)
                .then(thrown -> getElement().executeJs(FLUSH)
                        .then(flushed -> investigation.refreshNow()));
        Notification.show(message + " — in the browser. Nothing was thrown on "
                + "the server, so nothing was logged there.");
    }

    /**
     * Called by {@link #SIMULATE} once the browser has the server back and the
     * collector has been drained, so the outage that just ended is on screen
     * without this view polling for it — which it must not do, since a poll
     * that gets through ends the outage as far as the browser is concerned.
     * <p>
     * It recomputes a readout and nothing else, so an unsolicited call from the
     * page costs a refresh and can do nothing more.
     */
    @ClientCallable
    public void connectionRestored() {
        investigation.refreshNow();
    }

    // ---------- the investigation, revealed by the first problem ----------

    private Investigation buildInvestigation() {
        investigation.setId("investigation");
        investigation.onRefresh(this::refreshReadout);

        serverSide.setId("server-side");
        investigation.step("2 — The usual suspects see nothing", true,
                serverSide);

        verdict.setId("verdict");
        verdict.setWidthFull();
        investigation.step("3 — The kit's verdict", false, new Paragraph(
                "vaadin.client.errors, in step 4, says how many browser errors "
                        + "happened. These say which ones: the insights "
                        + "endpoint retains each error grouped by route, kind, "
                        + "source and frame with an occurrence count. A "
                        + "message cannot be a meter tag — it would be one "
                        + "time series per message — which is why the detail "
                        + "lives here and not there. The location is parsed out "
                        + "of the stack line the browser wrote and kept only "
                        + "when it is actually a location — a cross-origin "
                        + "script reports no filename, a rejection has none at "
                        + "all, and the page's own URL is not where the code "
                        + "is. \"Held offline\" is the giveaway that a report "
                        + "could not reach the server when it was raised. Not "
                        + "filtered to this screen: an error on any of them is "
                        + "one nobody is watching, and the route is part of the "
                        + "grouping key."),
                verdict);

        summary.setId("connection-summary");
        meters.setId("meter-table");
        investigation.step("4 — The raw meters, fleet-wide", false, summary,
                new Paragraph(
                        "What an outage looks like across the fleet. Downtime "
                                + "is tagged per state rather than per outage, "
                                + "because Flow enters reconnecting on the "
                                + "first failed request and connection-lost "
                                + "only after giving up retrying: the whole "
                                + "outage is the two summed, which is the row "
                                + "the application has to add up itself."),
                meters);

        investigation.step("5 — What the numbers still cannot tell you", false,
                caveats());

        return investigation;
    }

    private void refreshReadout() {
        Map<String, Object> payload = endpoint.section(SECTION);
        refreshServerSide();
        refreshVerdict(payload);
        refreshSummary();
        refreshMeters();
    }

    /**
     * Step 2: what a server-side reader has to go on. The error counter is the
     * first thing a developer checks, and for this story it is empty by
     * construction — the failures happened in a browser. The resync counters
     * are the only trace an outage leaves on the server, and only because the
     * kit publishes them.
     */
    private void refreshServerSide() {
        serverSide.removeAll();
        double failures = counterTotal(
                registry.find(ERRORS).tag(TAG_ROUTE, ROUTE).counters());
        serverSide.add(Telemetry.chip(ERRORS), new Span(" — "),
                Telemetry.timing("%.0f failure(s)".formatted(failures)),
                new Span(" on " + TAG_ROUTE + "=" + ROUTE
                        + ". A script that fails in the browser never reaches "
                        + "it, and neither does a browser that cannot reach the "
                        + "server: from the server's side the session simply "
                        + "goes quiet and then talks again. "));
        serverSide.add(new Span("The one server-side trace is "),
                Telemetry.chip(MeterNames.RESYNC), new Span(" — "),
                Telemetry.timing("%d resend(s)".formatted(
                        count(MeterNames.RESYNC, MeterNames.TAG_TYPE,
                                MeterNames.RESYNC_TYPE_RESEND))),
                new Span(", "),
                Telemetry.timing("%d resync(s)".formatted(
                        count(MeterNames.RESYNC, MeterNames.TAG_TYPE,
                                MeterNames.RESYNC_TYPE_RESYNC))),
                new Span(": messages a client re-sent having had no answer, and "
                        + "the full UI-state rebuilds it asked for after losing "
                        + "one. Flow handles both internally and says nothing, "
                        + "so without the kit even this is invisible."));
    }

    /**
     * Step 3: the {@code client-error} insights of the kit's payload. The
     * endpoint carries the other kinds too — failed and slow interactions, data
     * provider queries — which are UC6's and UC8's readouts, not this one.
     */
    private void refreshVerdict(@Nullable Map<String, Object> payload) {
        verdict.removeAll();
        if (!Insights.isActive(payload)) {
            verdict.add(empty("Nothing was watching: the kit registered no "
                    + "instrumentation, so there is nothing to report rather "
                    + "than nothing to find. In development mode this means no "
                    + "license key was found."));
            return;
        }
        List<Map<String, Object>> findings = Insights.of(payload).stream()
                .filter(insight -> CLIENT_ERROR.equals(insight.get("type")))
                .toList();
        if (findings.isEmpty()) {
            verdict.add(empty("No browser errors reported yet — they arrive on "
                    + "the collector's next flush, or with the one that follows "
                    + "a recovery. Show the stock levels above, or sync stock "
                    + "from the API."));
            return;
        }
        findings.forEach(insight -> {
            Map<String, Object> evidence = Insights.evidenceOf(insight);
            List<String> chips = new ArrayList<>();
            chips.add(TAG_ROUTE + "=" + Insights.text(evidence.get("route")));
            chips.add(Insights.text(evidence.get("kind")));
            // The location and the function name are separate fields on
            // purpose: the name is a string the page chose, so it is gated with
            // the message, while the location — which the kit's own summary
            // carries — is published whenever it is one.
            if (evidence.get("function") != null) {
                chips.add("in " + evidence.get("function"));
            }
            chips.add(Insights.text(evidence.get("occurrences")) + "×");
            long buffered = number(evidence.get("maxBufferedMs"));
            if (buffered > 0) {
                chips.add("held %.1f s offline".formatted(buffered / 1000d));
            }
            verdict.add(new InsightCard(insight, detailOf(evidence), chips));
        });
    }

    /**
     * The message, or the payload's own explanation of why there is none. The
     * kit says which case it is rather than leaving the field null, so a reader
     * can tell "not collected" from "the browser reported none".
     */
    private static String detailOf(Map<String, Object> evidence) {
        Object message = evidence.get("message");
        return message != null ? message.toString()
                : Insights.text(evidence.get("detail"));
    }

    private static Paragraph empty(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.addClassName("verdict-empty");
        return paragraph;
    }

    /** Step 4's lead: what the connection meters say, in a sentence. */
    private void refreshSummary() {
        summary.removeAll();
        long lost = count(MeterNames.CLIENT_CONNECTION, MeterNames.TAG_STATE,
                MeterNames.STATE_CONNECTION_LOST);
        long retrying = count(MeterNames.CLIENT_CONNECTION,
                MeterNames.TAG_STATE, MeterNames.STATE_RECONNECTING);
        long browserErrors = Math.round(counterTotal(
                registry.find(MeterNames.CLIENT_ERRORS).counters()));
        if (lost + retrying + browserErrors == 0) {
            summary.add(new Span("Nothing recorded since this server started — "
                    + "take the connection away with the demo rig, or break "
                    + "the stock chart above."));
            return;
        }
        double seconds = totalSeconds(MeterNames.STATE_CONNECTION_LOST)
                + totalSeconds(MeterNames.STATE_RECONNECTING);
        summary.add(new Span("Since this server started: "),
                Telemetry.timing("%d loss(es)".formatted(lost)),
                new Span(", "),
                Telemetry.timing("%d retry period(s)".formatted(retrying)),
                new Span(", "),
                Telemetry.timing("%.1f s".formatted(seconds)),
                new Span(" unable to reach the server, "),
                Telemetry.timing(
                        "%d browser error(s)".formatted(browserErrors)),
                new Span("."));
    }

    /** Step 4: every meter this story produces, read by its tags. */
    private void refreshMeters() {
        meters.setRows(List.of(
                transitions(MeterNames.STATE_CONNECTION_LOST,
                        "Flow reaches this state only after exhausting its "
                                + "retries, so each one is an outage a picker "
                                + "sat through"),
                transitions(MeterNames.STATE_RECONNECTING,
                        "Entered on the first failed request — the honest "
                                + "count of how often the connection faltered"),
                transitions(MeterNames.STATE_CONNECTED,
                        "Recoveries. Fewer of these than losses means tablets "
                                + "that never came back, and whose downtime is "
                                + "therefore unmeasured"),
                downtime(MeterNames.STATE_CONNECTION_LOST,
                        "Time spent on a server the browser had written off; "
                                + "measured on the browser's clock, since the "
                                + "report can only be sent once it is back"),
                downtime(MeterNames.STATE_RECONNECTING,
                        "Time spent retrying a network that hiccuped; a short "
                                + "outage never leaves this state, so it would "
                                + "be missed by an end-to-end measure"),
                wholeOutages(),
                counterRow(MeterNames.CLIENT_ERRORS, MeterNames.TAG_KIND,
                        MeterNames.KIND_UNCAUGHT,
                        "How many scripts threw; which ones is step 3, since a "
                                + "message cannot be a tag"),
                counterRow(MeterNames.CLIENT_ERRORS, MeterNames.TAG_KIND,
                        MeterNames.KIND_PROMISE,
                        "The other half of the same counter: rejections nobody "
                                + "handled"),
                counterRow(MeterNames.RESYNC, MeterNames.TAG_TYPE,
                        MeterNames.RESYNC_TYPE_RESEND,
                        "The server side of a lost message; Flow replays its "
                                + "cached response and says nothing"),
                counterRow(MeterNames.RESYNC, MeterNames.TAG_TYPE,
                        MeterNames.RESYNC_TYPE_RESYNC,
                        "The client gave up waiting and asked for the whole UI "
                                + "state again"),
                untaggedRow(MeterNames.CLIENT_DROPPED,
                        "Samples the server refused outright"),
                untaggedRow(MeterNames.CLIENT_THROTTLED,
                        "One outage flushes as one batch, which can outrun the "
                                + "per-session rate limit; the kit sends "
                                + "connection samples first, so what is lost "
                                + "here is timing")));
    }

    private MeterTable.Row transitions(String state, String reads) {
        return counterRow(MeterNames.CLIENT_CONNECTION, MeterNames.TAG_STATE,
                state, reads);
    }

    /**
     * A counter row. The count column carries the counter itself and the value
     * column stays empty: a counter has no value beside how many times it was
     * incremented. A meter that was never registered reads as a dash rather
     * than 0 — "nothing has happened" and "nothing is watching" are different
     * answers, and a server no browser has reported to has no such meter.
     */
    private MeterTable.Row counterRow(String meter, String tagKey,
            String tagValue, String reads) {
        Collection<Counter> counters = registry.find(meter)
                .tag(tagKey, tagValue).counters();
        return new MeterTable.Row(meter, tagKey + "=" + tagValue,
                counters.isEmpty() ? -1 : Math.round(counterTotal(counters)),
                "", reads);
    }

    private MeterTable.Row untaggedRow(String meter, String reads) {
        Collection<Counter> counters = registry.find(meter).counters();
        return new MeterTable.Row(meter, "—",
                counters.isEmpty() ? -1 : Math.round(counterTotal(counters)),
                "", reads);
    }

    /** The downtime timer for one unreachable state. */
    private MeterTable.Row downtime(String state, String reads) {
        Timer timer = timer(state);
        String value = timer == null || timer.count() == 0 ? ""
                : "%.1f s total, longest %.1f s".formatted(
                        timer.totalTime(TimeUnit.SECONDS),
                        timer.max(TimeUnit.SECONDS));
        return new MeterTable.Row(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                MeterNames.TAG_STATE + "=" + state,
                timer == null ? -1 : timer.count(), value, reads);
    }

    /**
     * The length of the outages end to end. The kit splits the timer by state
     * because the two states mean different things; adding them back is the
     * application's to do, and is the number an SLO would use.
     */
    private MeterTable.Row wholeOutages() {
        double seconds = totalSeconds(MeterNames.STATE_CONNECTION_LOST)
                + totalSeconds(MeterNames.STATE_RECONNECTING);
        return new MeterTable.Row(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                MeterNames.TAG_STATE + "=* summed", -1,
                seconds == 0 ? "" : "%.1f s across both states"
                        .formatted(seconds),
                "The length of an outage end to end, which is the sum and not "
                        + "either tag alone");
    }

    private @Nullable Timer timer(String state) {
        return registry.find(MeterNames.CLIENT_CONNECTION_DOWNTIME)
                .tag(MeterNames.TAG_STATE, state).timer();
    }

    private double totalSeconds(String state) {
        Timer timer = timer(state);
        return timer == null ? 0 : timer.totalTime(TimeUnit.SECONDS);
    }

    private long count(String meter, String tagKey, String tagValue) {
        return Math.round(counterTotal(
                registry.find(meter).tag(tagKey, tagValue).counters()));
    }

    private static double counterTotal(Collection<Counter> counters) {
        double total = 0;
        for (Counter counter : counters) {
            total += counter.count();
        }
        return total;
    }

    private static long number(@Nullable Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private static Div caveats() {
        UnorderedList list = new UnorderedList(new ListItem(
                "Downtime under-reports by construction: a tablet that never "
                        + "comes back reports nothing, and an outage spanning a "
                        + "reload keeps its count but loses its clock. Read the "
                        + "counts for how often, the timer only for how long "
                        + "the observed ones lasted."),
                new ListItem("A non-zero \"held offline\" says a report could "
                        + "not be delivered when it was raised — not that the "
                        + "error happened during an outage. A report taken "
                        + "while the browser was still connected accrues an "
                        + "outage that starts before the next flush."),
                new ListItem("Nothing here is measured by this application. "
                        + "The view had a shim for the connection state and "
                        + "then a listener for the error detail; the collector "
                        + "does both, on every screen rather than only on this "
                        + "one, so both are deleted (gap #5)."),
                new ListItem("Client samples arrive with no event to listen "
                        + "for. The collector's ingest raises nothing an "
                        + "application can subscribe to, so a live readout has "
                        + "to poll — which this view must not do, since a poll "
                        + "shortens the outages it reports — or be recomputed "
                        + "from the interactions, which is what this one does."),
                new ListItem("Draining the collector still needs a debug "
                        + "internal: the simulated recovery and the error "
                        + "buttons call window.__vaadinMicrometer.flush(), "
                        + "which the kit documents as debug-only (gap #12)."),
                new ListItem("The insights are an untyped JSON map in process. "
                        + "The payload is a good published contract for an "
                        + "agent; for a Java caller it means unchecked casts "
                        + "and string keys (gap #9)."),
                new ListItem("Push transport is still not instrumented on the "
                        + "client, so an app using @Push has no client-side "
                        + "view of its own delivery (gap #4)."));
        Div caveats = new Div(list);
        caveats.add(new Anchor("/actuator/vaadin/observability",
                "GET /actuator/vaadin/observability"));
        caveats.add(new Anchor(
                "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md",
                "See API-GAPS.md"));
        return caveats;
    }
}
