package com.example.uc5;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.views.MainLayout;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.spring.boot.VaadinObservabilityEndpoint;

/**
 * UC5 — Notice connection and client-side problems.
 * <p>
 * The class of failure that never reaches a server log: a user's browser loses
 * the server and gets it back, and a script fails in a tab nobody is watching.
 * The server sees a session that goes quiet and then talks again, and — for the
 * failed script — nothing at all.
 * <p>
 * All of the connection half is now the Observability Kit's, and this view only
 * reads it. The in-browser collector subscribes to Flow's own
 * {@code window.Vaadin.connectionState} and records
 * {@link MeterNames#CLIENT_CONNECTION} per transition and
 * {@link MeterNames#CLIENT_CONNECTION_DOWNTIME} for the time spent unable to
 * reach the server, both tagged {@link MeterNames#TAG_STATE}. Three properties
 * of those meters are what this view exists to make visible:
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
 * <li><b>The timer under-reports by construction.</b> A browser that never
 * comes back reports nothing, so the transition <em>count</em> is the honest
 * measure of how often, and the timer only of how long the observed ones
 * lasted.</li>
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
 * endpoint. This view reads the {@code client-error} insights out of that
 * payload, through the injected {@link VaadinObservabilityEndpoint} bean, and
 * gets for free everything an application-side listener had to do without:
 * grouping by route, kind, source and frame with an occurrence count; a
 * location parsed out of the stack line and validated as a location rather than
 * trusted; and {@code maxBufferedMs}, the offline time a report waited before
 * it could be delivered.
 * <p>
 * Two of those fields are gated. A browser error can quote anything the page
 * was working with, and a page can name a function anything, so the message and
 * the function name travel only when
 * {@code vaadin.observability.insights-details} is on — and for a browser error
 * that setting governs <em>collection</em>, not just retention: with it off the
 * browser never gathers them. This module turns it on for UC6, so they are
 * present here; the payload says which case it is rather than leaving a null.
 * <p>
 * <b>Why this view does not poll.</b> A poll is a UIDL request, so a polling
 * view probes the connection on every tick: the loading round trip itself is
 * ignored by the collector, but a poll that gets through ends the outage as far
 * as the browser is concerned, and a polling tab therefore reports shorter
 * downtime than a passive one on the same network. The kit's own README says as
 * much. Nothing pushes the other way either — the collector's ingest raises no
 * event an application can subscribe to — so the readout is refreshed by hand,
 * which is also what keeps it from probing the connection it is measuring.
 *
 * @see <a href=
 *      "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md">API-GAPS.md</a>
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Connection & client problems")
@Menu(order = 5, title = "UC5 — Connection & client problems")
public class ConnectionInsightsView extends VerticalLayout {

    /** How long the simulated outage lasts, in milliseconds. */
    private static final int OUTAGE_MILLIS = 3000;

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
     */
    private static final String SIMULATE = """
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

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    /** The endpoint's selector, i.e. {@code /actuator/vaadin/observability}. */
    private static final String SECTION = "observability";

    /** The insight type this view reads; the payload carries others. */
    private static final String CLIENT_ERROR = "client-error";

    /** One row of the meter readout. */
    public record Stat(String signal, String value, String meter,
            String reads) {
    }

    /**
     * One {@code client-error} insight, flattened for display.
     *
     * @param where
     *            the location the kit parsed out of the first stack frame, or
     *            the script the browser named, or nothing — it publishes these
     *            only when what the browser said is actually a location
     * @param message
     *            the message, or the payload's own sentence saying why there is
     *            none: not collected, or collected and the browser had none
     * @param maxBufferedMillis
     *            the longest any occurrence in this group waited for its
     *            browser to reach the server. Non-zero means at least one of
     *            them could not be delivered when it was raised
     */
    public record ErrorInsight(String route, String kind, String where,
            String message, String function, long occurrences,
            long maxBufferedMillis, String lastSeen) {
    }

    /** The whole readout at a point in time. */
    public record Readout(String status, boolean degraded, List<Stat> meters,
            List<ErrorInsight> errors, String insights) {
        static final Readout EMPTY = new Readout("", false, List.of(),
                List.of(), "");
    }

    private final transient MeterRegistry registry;
    private final transient VaadinObservabilityEndpoint endpoint;
    private final ValueSignal<Readout> readout = new ValueSignal<>(
            Readout.EMPTY);
    private final Span status = new Span();
    private final Span insightsStatus = new Span();
    private final Grid<Stat> meters = new Grid<>();
    private final Grid<ErrorInsight> errors = new Grid<>();

    /**
     * @param registry
     *            the application's registry, which the kit's binders — the
     *            in-browser collector's included — publish into
     * @param endpoint
     *            the kit's insights endpoint bean, so the errors below are the
     *            very same records {@code GET /actuator/vaadin/observability}
     *            serves
     */
    public ConnectionInsightsView(MeterRegistry registry,
            VaadinObservabilityEndpoint endpoint) {
        this.registry = registry;
        this.endpoint = endpoint;

        add(new H1("UC5 — Notice connection and client-side problems"));
        add(new Paragraph("A user's browser loses the server and gets it back; "
                + "a script fails in a tab nobody is watching. Neither reaches "
                + "a server log. Every meter below is the Observability Kit's: "
                + "its in-browser collector subscribes to Flow's own "
                + "connection-state store and reports what it saw once the "
                + "browser can talk again. Simulate a problem with the "
                + "buttons, or take the network away in devtools — both go "
                + "through the same store."));

        status.setId("connection-status");
        status.getElement().getThemeList().add("badge");
        add(status);

        add(actions());

        meters.addColumn(Stat::signal).setHeader("Signal").setFlexGrow(1);
        meters.addColumn(Stat::value).setHeader("Value").setAutoWidth(true);
        meters.addColumn(Stat::meter).setHeader("Meter").setAutoWidth(true);
        meters.addColumn(Stat::reads).setHeader("What it tells you")
                .setFlexGrow(2);
        meters.setAllRowsVisible(true);
        meters.setId("problem-meters");
        meters.addThemeName("wrap-cell-content");
        add(new H2("What the kit records"), meters);

        errors.addColumn(ErrorInsight::lastSeen).setHeader("Last seen")
                .setAutoWidth(true);
        errors.addColumn(ErrorInsight::route).setHeader("Route")
                .setAutoWidth(true);
        errors.addColumn(ErrorInsight::kind).setHeader("Kind")
                .setAutoWidth(true);
        errors.addColumn(ErrorInsight::message).setHeader("Message")
                .setFlexGrow(2);
        // The location and the name are separate fields on purpose: the name
        // is a string the page chose, so it is gated with the message, while
        // the location is published whenever it is one.
        errors.addColumn(ErrorInsight::where).setHeader("Where").setFlexGrow(2);
        errors.addColumn(ErrorInsight::function).setHeader("In function")
                .setAutoWidth(true);
        errors.addColumn(ErrorInsight::occurrences).setHeader("Occurrences")
                .setAutoWidth(true);
        errors.addColumn(ConnectionInsightsView::held)
                .setHeader("Report held offline").setAutoWidth(true);
        errors.setAllRowsVisible(true);
        errors.setId("error-detail");
        errors.addThemeName("wrap-cell-content");
        add(new H2("The errors themselves, as insights"), errors);

        insightsStatus.setId("insights-status");
        insightsStatus.getStyle().set("font-style", "italic");
        add(insightsStatus);
        add(new Paragraph("The counter above says how many browser errors "
                + "happened. These rows say which ones, grouped by route, kind "
                + "and location with an occurrence count — the same records "
                + "GET /actuator/vaadin/observability serves, read here from "
                + "the endpoint bean rather than over HTTP. The location is "
                + "parsed out of the stack line the browser wrote and kept "
                + "only when it is actually a location: a cross-origin script "
                + "reports no filename, a rejection has none at all, and the "
                + "page's own URL is not where the code is, so those are "
                + "dropped rather than substituted. \"Report held offline\" is "
                + "the giveaway that an error could not be told to the server "
                + "when it was raised."));

        // The primary signal-bound containers: badge and both grids repaint
        // from one snapshot, so the meters and the detail cannot disagree.
        Signal.effect(status, () -> {
            Readout current = readout.get();
            status.setText(current.status());
            ThemeList themes = status.getElement().getThemeList();
            themes.set("error", current.degraded());
        });
        Signal.effect(meters, () -> meters.setItems(readout.get().meters()));
        Signal.effect(errors, () -> {
            errors.setItems(readout.get().errors());
            insightsStatus.setText(readout.get().insights());
        });

        add(callout());

        recompute();
    }

    private HorizontalLayout actions() {
        Button lose = new Button("Simulate a 3 s connection loss",
                event -> simulate(MeterNames.STATE_CONNECTION_LOST,
                        OUTAGE_MILLIS,
                        "Connection lost for 3 s. Leave the page alone: any "
                                + "request that gets through ends the outage "
                                + "as far as the browser is concerned."));
        lose.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
        lose.setId("simulate-loss");

        // The other unreachable state, and the one a short real outage
        // actually spends its time in.
        Button reconnect = new Button("Simulate 1.5 s reconnecting",
                event -> simulate(MeterNames.STATE_RECONNECTING, 1500,
                        "Reconnecting for 1.5 s — the state a short outage "
                                + "recovers from without ever being given up "
                                + "on."));
        reconnect.addThemeVariants(ButtonVariant.WARNING,
                ButtonVariant.PRIMARY);
        reconnect.setId("simulate-reconnecting");

        Button error = new Button("Throw an uncaught browser error",
                event -> raise(THROW, "UC5: rendering the sales chart failed"));
        error.setId("throw-error");

        Button rejection = new Button("Reject a promise",
                event -> raise(REJECT, "UC5: fetching /api/quotes failed"));
        rejection.setId("reject-promise");

        Button refresh = new Button("Refresh", event -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(FLUSH)
                    .then(ignored -> recompute()));
            recompute();
        });
        refresh.setId("refresh");

        HorizontalLayout actions = new HorizontalLayout(lose, reconnect, error,
                rejection, refresh);
        actions.setWrap(true);
        return actions;
    }

    private void simulate(String state, int millis, String message) {
        getUI().ifPresent(
                ui -> ui.getPage().executeJs(SIMULATE, state, millis));
        Notification.show(message);
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
     * it ever does not hold, the refresh button is the backstop.
     */
    private void raise(String script, String message) {
        getUI().ifPresent(ui -> ui.getPage().executeJs(script, message)
                .then(thrown -> ui.getPage().executeJs(FLUSH)
                        .then(flushed -> recompute())));
        Notification.show(message);
    }

    /**
     * Rebuilds the snapshot from the registry and the endpoint payload. Called
     * from the click handlers, on this UI's thread, so the signal write is
     * safe.
     */
    void recompute() {
        Map<String, Object> payload = endpoint.section(SECTION);
        if (payload == null) {
            payload = Map.of();
        }
        List<ErrorInsight> insights = errorInsights(payload);
        readout.set(new Readout(statusOf(), degraded(), stats(), insights,
                insightsStatusOf(payload, insights)));
    }

    /**
     * Picks the {@code client-error} insights out of the payload. The endpoint
     * carries the other kinds too — failed and slow interactions, data provider
     * queries — which is UC6's readout, not this one.
     */
    private static List<ErrorInsight> errorInsights(
            Map<String, Object> payload) {
        List<ErrorInsight> rows = new ArrayList<>();
        for (Map<String, Object> insight : insightsOf(payload)) {
            if (!CLIENT_ERROR.equals(insight.get("type"))) {
                continue;
            }
            Map<String, Object> evidence = evidenceOf(insight);
            rows.add(new ErrorInsight(text(evidence.get("route")),
                    text(evidence.get("kind")), where(evidence),
                    message(evidence), text(evidence.get("function")),
                    number(evidence.get("occurrences")),
                    number(evidence.get("maxBufferedMs")),
                    time(evidence.get("lastSeen"))));
        }
        return rows;
    }

    /**
     * The location, preferring the frame the kit parsed out of the stack over
     * the script the browser named. Either may be absent: both are published
     * only when what the browser reported is actually a location.
     */
    private static String where(Map<String, Object> evidence) {
        Object frame = evidence.get("frame");
        return frame != null ? frame.toString() : text(evidence.get("source"));
    }

    /**
     * The message, or the payload's own explanation of why there is none. The
     * kit says which case it is rather than leaving the field null, so a reader
     * can tell "not collected" from "the browser reported none".
     */
    private static String message(Map<String, Object> evidence) {
        Object message = evidence.get("message");
        return message != null ? message.toString()
                : text(evidence.get("detail"));
    }

    private String insightsStatusOf(Map<String, Object> payload,
            List<ErrorInsight> insights) {
        if (!"active".equals(payload.get("instrumentation"))) {
            return "Observability Kit registered no instrumentation, so there "
                    + "is nothing to report. In development mode this means no "
                    + "license key was found.";
        }
        if (insights.isEmpty()) {
            return "No browser errors reported yet. They arrive on the "
                    + "collector's next flush, or with the one that follows a "
                    + "recovery.";
        }
        return insights.size() + " grouped insight(s). At most 20 travel in "
                + "one payload, most-reported first, because two parts of the "
                + "grouping key are the browser's to choose.";
    }

    private List<Stat> stats() {
        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat("Browsers that gave up on the server",
                counter(MeterNames.CLIENT_CONNECTION, MeterNames.TAG_STATE,
                        MeterNames.STATE_CONNECTION_LOST),
                tagged(MeterNames.CLIENT_CONNECTION,
                        MeterNames.STATE_CONNECTION_LOST),
                "Flow reaches this state only after exhausting its retries, so "
                        + "each one is an outage a user sat through"));
        stats.add(new Stat("Browsers that started retrying",
                counter(MeterNames.CLIENT_CONNECTION, MeterNames.TAG_STATE,
                        MeterNames.STATE_RECONNECTING),
                tagged(MeterNames.CLIENT_CONNECTION,
                        MeterNames.STATE_RECONNECTING),
                "Entered on the first failed request — the honest count of how "
                        + "often the connection faltered"));
        stats.add(new Stat("Recoveries",
                counter(MeterNames.CLIENT_CONNECTION, MeterNames.TAG_STATE,
                        MeterNames.STATE_CONNECTED),
                tagged(MeterNames.CLIENT_CONNECTION,
                        MeterNames.STATE_CONNECTED),
                "Fewer recoveries than losses means browsers that never came "
                        + "back, and whose downtime is therefore unmeasured"));
        stats.add(new Stat("Time spent given up on",
                downtime(MeterNames.STATE_CONNECTION_LOST),
                tagged(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                        MeterNames.STATE_CONNECTION_LOST),
                "A server the browser had written off; measured on the "
                        + "browser's clock, since the report can only be sent "
                        + "once it is back"));
        stats.add(new Stat("Time spent retrying",
                downtime(MeterNames.STATE_RECONNECTING),
                tagged(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                        MeterNames.STATE_RECONNECTING),
                "A network that hiccuped; a short outage never leaves this "
                        + "state, so it would be missed by an end-to-end "
                        + "measure"));
        stats.add(new Stat("Whole outages, both states summed", wholeOutages(),
                MeterNames.CLIENT_CONNECTION_DOWNTIME + " (sum of tags)",
                "The length of an outage end to end, which is the sum and not "
                        + "either tag alone"));
        stats.add(new Stat("Uncaught browser errors",
                counter(MeterNames.CLIENT_ERRORS, MeterNames.TAG_KIND,
                        MeterNames.KIND_UNCAUGHT),
                tagged(MeterNames.CLIENT_ERRORS, MeterNames.KIND_UNCAUGHT),
                "How many; which ones is the insight table below, since a "
                        + "message cannot be a tag"));
        stats.add(new Stat("Unhandled promise rejections",
                counter(MeterNames.CLIENT_ERRORS, MeterNames.TAG_KIND,
                        MeterNames.KIND_PROMISE),
                tagged(MeterNames.CLIENT_ERRORS, MeterNames.KIND_PROMISE),
                "The other half of the same counter"));
        stats.add(new Stat("Messages the client re-sent, having had no answer",
                counter(MeterNames.RESYNC, MeterNames.TAG_TYPE,
                        MeterNames.RESYNC_TYPE_RESEND),
                tagged(MeterNames.RESYNC, MeterNames.RESYNC_TYPE_RESEND),
                "The server side of a lost message; Flow replays its cached "
                        + "response and says nothing"));
        stats.add(new Stat("Full UI-state resynchronizations",
                counter(MeterNames.RESYNC, MeterNames.TAG_TYPE,
                        MeterNames.RESYNC_TYPE_RESYNC),
                tagged(MeterNames.RESYNC, MeterNames.RESYNC_TYPE_RESYNC),
                "The client gave up waiting and asked for the whole UI state "
                        + "again"));
        stats.add(new Stat("Client samples refused",
                counter(MeterNames.CLIENT_DROPPED) + " dropped, "
                        + counter(MeterNames.CLIENT_THROTTLED) + " throttled",
                MeterNames.CLIENT_DROPPED + " / " + MeterNames.CLIENT_THROTTLED,
                "One outage flushes as one batch, which can outrun the "
                        + "per-session rate limit; the kit sends connection "
                        + "samples first, so what is lost here is timing"));
        return stats;
    }

    /**
     * The badge: what the kit's connection meters say has happened since this
     * server started, in a sentence.
     */
    private String statusOf() {
        long lost = count(MeterNames.CLIENT_CONNECTION,
                MeterNames.STATE_CONNECTION_LOST);
        long retrying = count(MeterNames.CLIENT_CONNECTION,
                MeterNames.STATE_RECONNECTING);
        long browserErrors = Math.round(counterTotal(
                registry.find(MeterNames.CLIENT_ERRORS).counters()));
        if (lost + retrying + browserErrors == 0) {
            return "Nothing recorded since this server started. Simulate a "
                    + "problem, or go offline in devtools and come back.";
        }
        double seconds = totalSeconds(MeterNames.STATE_CONNECTION_LOST)
                + totalSeconds(MeterNames.STATE_RECONNECTING);
        return ("%d loss(es), %d retry period(s) totalling %.1f s, "
                + "%d browser error(s) since this server started")
                .formatted(lost, retrying, seconds, browserErrors);
    }

    /** Red once anything has gone wrong; there is no good news to report. */
    private boolean degraded() {
        return count(MeterNames.CLIENT_CONNECTION,
                MeterNames.STATE_CONNECTION_LOST)
                + count(MeterNames.CLIENT_CONNECTION,
                        MeterNames.STATE_RECONNECTING)
                + Math.round(counterTotal(registry
                        .find(MeterNames.CLIENT_ERRORS).counters())) > 0;
    }

    private static String tagged(String meter, String tagValue) {
        return meter + " {" + tagValue + "}";
    }

    /**
     * Sums a counter across all of its series. A meter that was never
     * registered reads as a dash rather than 0: "nothing has happened" and
     * "nothing is watching" are different answers.
     */
    private String counter(String meter) {
        return format(registry.find(meter).counters());
    }

    /** The same sum, narrowed to one tag. */
    private String counter(String meter, String tagKey, String tagValue) {
        return format(registry.find(meter).tag(tagKey, tagValue).counters());
    }

    private static String format(Collection<Counter> counters) {
        return counters.isEmpty() ? "—"
                : Long.toString(Math.round(counterTotal(counters)));
    }

    private long count(String meter, String state) {
        return Math.round(counterTotal(registry.find(meter)
                .tag(MeterNames.TAG_STATE, state).counters()));
    }

    private static double counterTotal(Collection<Counter> counters) {
        double total = 0;
        for (Counter counter : counters) {
            total += counter.count();
        }
        return total;
    }

    /** The downtime timer for one unreachable state. */
    private String downtime(String state) {
        Timer timer = registry.find(MeterNames.CLIENT_CONNECTION_DOWNTIME)
                .tag(MeterNames.TAG_STATE, state).timer();
        if (timer == null || timer.count() == 0) {
            return "—";
        }
        return "%d period(s), %.1f s total, longest %.1f s".formatted(
                timer.count(), timer.totalTime(TimeUnit.SECONDS),
                timer.max(TimeUnit.SECONDS));
    }

    /**
     * The length of the outages end to end. The kit splits the timer by state
     * because the two states mean different things; adding them back is the
     * application's to do, and is the number an SLO would use.
     */
    private String wholeOutages() {
        double seconds = totalSeconds(MeterNames.STATE_CONNECTION_LOST)
                + totalSeconds(MeterNames.STATE_RECONNECTING);
        return seconds == 0 ? "—"
                : "%.1f s across both states".formatted(seconds);
    }

    private double totalSeconds(String state) {
        Timer timer = registry.find(MeterNames.CLIENT_CONNECTION_DOWNTIME)
                .tag(MeterNames.TAG_STATE, state).timer();
        return timer == null ? 0 : timer.totalTime(TimeUnit.SECONDS);
    }

    /** How long the worst occurrence in a group waited to be deliverable. */
    private static String held(ErrorInsight insight) {
        return insight.maxBufferedMillis() == 0 ? "—"
                : "%.1f s".formatted(insight.maxBufferedMillis() / 1000d);
    }

    // ---------- reading the endpoint payload ----------
    //
    // The payload is shaped for the endpoint, so an in-app consumer casts its
    // way through nested maps and string keys with no compile-time contract
    // (API-GAPS.md #9). UC6 does the same for the interaction insights.

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> insightsOf(
            Map<String, Object> payload) {
        return payload.get("insights") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> evidenceOf(Map<String, Object> insight) {
        return insight.get("evidence") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private static String text(@Nullable Object value) {
        return value == null ? "—" : value.toString();
    }

    private static long number(@Nullable Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    /**
     * The payload's timestamps are ISO instants of when the server received a
     * report — which can lag the error by a whole outage, so the column is
     * headed "last seen" rather than "happened".
     */
    private static String time(@Nullable Object value) {
        if (value == null) {
            return "—";
        }
        try {
            return TIME.format(Instant.parse(value.toString()));
        } catch (RuntimeException notAnInstant) {
            return value.toString();
        }
    }

    private static Details callout() {
        UnorderedList list = new UnorderedList(new ListItem(
                "Nothing here is measured by this application any more. The "
                        + "view had a shim for the connection state and then a "
                        + "listener for the error detail; the collector does "
                        + "both, on every UI rather than only on this route, "
                        + "so both are deleted (gap #5)."),
                new ListItem("The insights are an untyped JSON map in process. "
                        + "The payload is a good published contract for an "
                        + "agent; for a Java caller it means unchecked casts "
                        + "and string keys, which is what the bottom of this "
                        + "class does (gap #9)."),
                new ListItem("Client samples arrive with no event to listen "
                        + "for. The collector's ingest raises nothing an "
                        + "application can subscribe to, so a live readout has "
                        + "to poll — which this view must not do, since a poll "
                        + "shortens the outages it reports — or be refreshed by "
                        + "hand, which is why there is a button."),
                new ListItem("Draining the collector still needs a debug "
                        + "internal: the refresh and error buttons call "
                        + "window.__vaadinMicrometer.flush(), which the kit "
                        + "documents as debug-only (gap #12)."),
                new ListItem("Downtime under-reports by construction: a "
                        + "browser that never comes back reports nothing, and "
                        + "an outage spanning a reload keeps its count but "
                        + "loses its clock. Read the counts for how often, the "
                        + "timer only for how long the observed ones lasted."),
                new ListItem("A non-zero \"report held offline\" says a report "
                        + "could not be delivered when it was raised — not "
                        + "that the error happened during an outage. A report "
                        + "taken while the browser was still connected accrues "
                        + "an outage that starts before the next flush."),
                new ListItem("Push transport is still not instrumented on the "
                        + "client, so an app using @Push has no client-side "
                        + "view of its own delivery (gap #4)."));
        Details details = new Details("What this can't show yet (and why)",
                list);
        details.add(new Anchor("/actuator/vaadin/observability",
                "GET /actuator/vaadin/observability"));
        details.add(new Anchor(
                "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md",
                "See API-GAPS.md"));
        return details;
    }
}
