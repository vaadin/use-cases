package com.example.uc2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.acme.AppWindow;
import com.example.acme.DemoRig;
import com.example.acme.Investigation;
import com.example.acme.MeterTable;
import com.example.acme.Telemetry;
import com.example.uc2.ProductCatalogService.CatalogLoad;
import com.example.uc2.ProductCatalogService.Line;
import com.example.views.MainLayout;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;

/**
 * UC2 — the whole app hiccups whenever someone opens Acme's catalog page: is
 * the app healthy, and what is it doing?
 * <p>
 * The view opens with the story: an {@link AppWindow} showing the inventory
 * page, whose catalog refresh runs the classic N+1 join-table fetch (see
 * {@link Product}: an eager, unbatched many-to-many). The first load reveals
 * the {@link Investigation}, which is <em>live</em>: the UI polls every couple
 * of seconds and the readout recomputes on every tick, so other sessions
 * coming and going, memory moving and the browser-collected samples all show
 * up without a click.
 * <p>
 * Its steps: <b>2)</b> the vital signs look fine — the application's own
 * signals read straight from its {@link MeterRegistry}, the same registry the
 * Observability Kit binders publish into: {@code vaadin.sessions.active} and
 * {@code vaadin.ui.active}, the JVM heap gauges, {@code vaadin.request.duration}
 * and {@code vaadin.rpc.duration}, the browser's own {@code vaadin.client.*}
 * load and paint signals (POSTed back by the in-browser collector),
 * {@code vaadin.errors} and {@code vaadin.client.errors}, plus a connection
 * badge; <b>3)</b> the database gives it away — the kit's
 * {@code vaadin.db.fetch.rows} summary ({@code vaadin.observability.database
 * =true}), scoped to this route and bracketed around the load, counts N+1
 * result-set fetches for N products; <b>4)</b> the fix, verified — the
 * {@link DemoRig}'s join-fetch switch brings the categories along in one
 * query, and the load history shows the fetch count drop to one.
 * <p>
 * Connection status is the one signal with no meter behind it (see
 * {@code API-GAPS.md} #5): the browser's {@code online}/{@code reconnecting}
 * state is never recorded server-side. So instead of claiming to know it, the
 * badge reports the only related thing the server can observe: the cadence of
 * this UI's own poll requests — neutral before the first tick, green while
 * ticks arrive on schedule, red after a late one. The push mode and transport
 * come from the public {@link PushConfiguration}.
 * <p>
 * There is no {@code vaadin.client.rpc.duration}: the kit measures the
 * per-request round-trip server-side only; the client collector emits load-
 * and paint-oriented meters.
 */
@Route(value = ApplicationHealthView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Application health")
@Menu(order = 2, title = "UC2 — Application health")
public class ApplicationHealthView extends VerticalLayout {

    /**
     * The route template, which is also the {@code route} tag on the kit's
     * database fetch summary. Named after the Acme screen; the {@code uc2}
     * alias keeps the numbered URL working without appearing in the telemetry.
     */
    static final String ROUTE = "inventory";

    private static final String SESSIONS = "vaadin.sessions.active";
    private static final String UIS = "vaadin.ui.active";
    private static final String HEAP_USED = "jvm.memory.used";
    private static final String HEAP_MAX = "jvm.memory.max";
    private static final String REQUEST = "vaadin.request.duration";
    private static final String RPC = "vaadin.rpc.duration";
    private static final String CLIENT_BOOTSTRAP = "vaadin.client.bootstrap.duration";
    private static final String CLIENT_NAVIGATION = "vaadin.client.navigation.duration";
    private static final String CLIENT_LCP = "vaadin.client.web_vitals.lcp";
    private static final String CLIENT_FCP = "vaadin.client.web_vitals.fcp";
    private static final String ERRORS = "vaadin.errors";
    private static final String CLIENT_ERRORS = "vaadin.client.errors";
    /**
     * The Observability Kit's database meter: a DistributionSummary of the rows
     * read per JDBC result set, tagged by route, recorded when
     * vaadin.observability.database=true.
     */
    static final String DB_FETCH_ROWS = "vaadin.db.fetch.rows";
    private static final String TAG_ROUTE = "route";
    private static final int POLL_MILLIS = 2000;

    /**
     * How current the readout is, derived from the observed refresh cadence —
     * the closest thing to a connection state the server can see (there is no
     * connection-state meter; see {@code API-GAPS.md} #5).
     */
    public enum Channel {
        /** No cadence observed yet, so liveness is genuinely unknown. */
        UNKNOWN,
        /** The latest refresh arrived within the expected poll window. */
        LIVE,
        /** The latest refresh was late: updates had stopped arriving. */
        RESUMED
    }

    /**
     * One catalog load as seen through the kit's database meter: how many
     * products came back and how many separate result-set fetches (and rows)
     * the kit attributed to this route during the load. {@code monitored} is
     * false when the running kit build has no database feature, so the meter
     * never appeared.
     */
    record Load(boolean joinFetch, int products, int categories,
            long fetches, long rows, boolean monitored) {

        /**
         * More fetches than products is the signature of an unbatched N+1: one
         * query for the products plus one per-product collection fetch.
         */
        boolean looksLikeNPlusOne() {
            return monitored && products > 0 && fetches > products;
        }
    }

    private final transient MeterRegistry registry;
    private final transient ProductCatalogService catalog;
    private final Investigation investigation = new Investigation(
            "Felt the hiccup? Here is what the app was doing. This readout is "
                    + "live: it refreshes every " + (POLL_MILLIS / 1000)
                    + " s on its own.");
    private final Table catalogTable = new Table();
    private final Span catalogSummary = new Span();
    private final Checkbox joinFetch = new Checkbox(
            "Fetch categories with the products (join fetch) — the fix");
    private final Span status = new Span();
    private final MeterTable vitals = new MeterTable("Samples");
    private final Paragraph catalogResult = new Paragraph();
    private final Table loadHistory = new Table();
    private final List<Load> loads = new ArrayList<>();
    private int refreshes;
    private long lastRefreshAt;
    private @Nullable Registration pollRegistration;

    public ApplicationHealthView(MeterRegistry registry,
            ProductCatalogService catalog) {
        this.registry = registry;
        this.catalog = catalog;

        add(new H1("UC2 — Why does the whole app hiccup when someone opens "
                + "the catalog?"));
        add(new Paragraph(
                "Every morning Acme's inventory manager refreshes the product "
                        + "catalog, and every morning the app gets sluggish for "
                        + "everyone for a moment. Is the app healthy? What is it "
                        + "doing? Reproduce it below, then read the app's own "
                        + "vital signs — and what the database was up to."));

        add(new H3("1 — Open the catalog"));
        add(new Paragraph(
                "Refresh the catalog. It is a small one, and it still takes a "
                        + "moment — do it a couple of times."));

        add(buildInventory());
        add(buildDemoRig());
        add(buildInvestigation());

        investigation.refreshNow();
    }

    // ---------- the Acme inventory page and its demo rig ----------

    private AppWindow buildInventory() {
        Button refresh = new Button("Refresh catalog", e -> loadCatalog());
        refresh.addThemeVariants(ButtonVariant.PRIMARY);
        refresh.setId("load-catalog");

        catalogSummary.setText("Catalog not loaded yet.");
        catalogSummary.addClassName("catalog-summary");

        catalogTable.setId("catalog");
        catalogTable.addClassName("order-lines");
        catalogTable.setWidthFull();
        catalogTable.addHeaderRow("Product", "Categories");
        TableRow emptyRow = new TableRow();
        emptyRow.addDataCell("Refresh to fetch the catalog from the database.")
                .setColspan(2);
        emptyRow.addClassName("order-empty");
        catalogTable.addRows(emptyRow);

        return new AppWindow("Acme Supply — Inventory", ROUTE,
                new HorizontalLayout(Alignment.CENTER, refresh,
                        catalogSummary),
                catalogTable);
    }

    /**
     * The fix as a switch, and the client-collector flush. The flush asks the
     * in-browser collector to POST its buffer now instead of waiting for its
     * periodic timer, through {@code window.__vaadinMicrometer.flush()} — an
     * internal the kit documents as debug-only (see {@code API-GAPS.md} #12),
     * so the call is guarded and degrades to a no-op if it disappears. The
     * flushed samples arrive in a follow-up request, so the readout is
     * recomputed once the script returns; the poll backstops anything later.
     */
    private DemoRig buildDemoRig() {
        joinFetch.setId("join-fetch");

        Button flush = new Button("Flush client metrics now",
                e -> getUI().ifPresent(ui -> ui.getPage()
                        .executeJs("window.__vaadinMicrometer && "
                                + "window.__vaadinMicrometer.flush();")
                        .then(ignored -> investigation.refreshNow())));
        flush.setId("flush-client");

        DemoRig rig = new DemoRig(joinFetch, flush);
        rig.setId("simulation-rig");
        return rig;
    }

    /**
     * Loads the catalog and attributes the kit's recorded fetches to this click
     * by reading this route's {@code vaadin.db.fetch.rows} summary immediately
     * before and after the load. The first load reveals the investigation.
     */
    private void loadCatalog() {
        boolean fix = Boolean.TRUE.equals(joinFetch.getValue());
        FetchTotals before = fetchTotals();
        CatalogLoad load = catalog.loadCatalog(fix);
        FetchTotals after = fetchTotals();
        loads.add(new Load(fix, load.products(), load.categories(),
                after.count() - before.count(),
                Math.round(after.rows() - before.rows()), after.present()));

        List.copyOf(catalogTable.getBodyRows())
                .forEach(TableRow::removeFromParent);
        for (Line line : load.lines()) {
            catalogTable.addRow(line.name(), line.categories());
        }
        catalogSummary.setText("%d products in %d category links"
                .formatted(load.products(), load.categories()));

        investigation.reveal();
    }

    // ---------- the investigation, revealed by the first catalog load ------

    private Investigation buildInvestigation() {
        investigation.setId("investigation");
        investigation.onRefresh(this::recompute);

        status.setId("connection-status");
        status.getElement().getThemeList().add("badge");
        vitals.setId("vital-signs");
        investigation.step("2 — The vital signs look fine", true, status,
                new Paragraph(
                        "The application's own signals, read from its "
                                + "MeterRegistry — the one the kit's binders "
                                + "publish into. Nothing here says \"database\"."),
                vitals);

        catalogResult.setId("catalog-result");
        investigation.step("3 — The database gives it away", false,
                catalogResult);

        loadHistory.setId("load-history");
        loadHistory.addClassName("order-lines");
        loadHistory.setWidthFull();
        loadHistory.addHeaderRow("Load", "Products", "Fetches", "Rows");
        Paragraph fixLead = new Paragraph();
        fixLead.add(new Span(
                "Flip the demo rig's join-fetch switch and refresh the catalog "
                        + "again: one query brings the categories along with "
                        + "the products, and "),
                Telemetry.chip(DB_FETCH_ROWS),
                new Span(" counts a single fetch. Every load so far:"));
        investigation.step("4 — The fix, verified", false, fixLead,
                loadHistory);

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

    private void recompute() {
        refreshes++;
        long now = System.currentTimeMillis();
        long gapMillis = lastRefreshAt == 0 ? 0 : now - lastRefreshAt;
        Channel channel = channel(lastRefreshAt, now, POLL_MILLIS);
        lastRefreshAt = now;

        refreshStatus(channel, gapMillis);
        refreshVitals();
        refreshCatalogResult();
        refreshLoadHistory();
    }

    /**
     * Derives the channel state from the observed refresh cadence: the view
     * polls every {@code pollMillis}, so a tick within two poll intervals of
     * the previous one means updates are flowing, and a later one means they
     * had stopped in between. Pure and package-private so all three branches
     * are exercised in tests without a clock or a browser.
     */
    static Channel channel(long previousRefreshAt, long now, int pollMillis) {
        if (previousRefreshAt == 0) {
            return Channel.UNKNOWN;
        }
        return now - previousRefreshAt > 2L * pollMillis ? Channel.RESUMED
                : Channel.LIVE;
    }

    /**
     * The server's view of the connection: only what it can see — whether this
     * UI's poll requests are still arriving on schedule — never a claim that
     * the browser is online.
     */
    private void refreshStatus(Channel channel, long gapMillis) {
        String state = switch (channel) {
        case UNKNOWN -> "Waiting for the first live update";
        case LIVE -> "Live — updates arriving every " + (POLL_MILLIS / 1000)
                + " s";
        case RESUMED -> String.format("Resumed after %.1f s without updates",
                gapMillis / 1000d);
        };
        UI ui = UI.getCurrent();
        String push = "push n/a";
        if (ui != null) {
            PushConfiguration pc = ui.getPushConfiguration();
            push = "push " + pc.getPushMode() + " over " + pc.getTransport();
        }
        status.setText(state + " (" + push + "); " + refreshes
                + " refreshes this session");
        // Neutral until a cadence is known, green while ticks arrive on time,
        // red for a tick that arrives late.
        ThemeList themes = status.getElement().getThemeList();
        themes.set("success", channel == Channel.LIVE);
        themes.set("error", channel == Channel.RESUMED);
    }

    /** Step 2: the vital signs. */
    private void refreshVitals() {
        List<MeterTable.Row> rows = new ArrayList<>();
        rows.add(gaugeRow(SESSIONS, count(gaugeValue(SESSIONS)),
                "Signed-in users (sessions)"));
        rows.add(gaugeRow(UIS, count(gaugeValue(UIS)),
                "Open browser tabs (UIs)"));
        rows.add(new MeterTable.Row(HEAP_USED, "area=heap", -1,
                megabytes(gaugeSum(HEAP_USED, "heap")), "Heap in use"));
        rows.add(new MeterTable.Row(HEAP_MAX, "area=heap", -1,
                megabytes(gaugeSum(HEAP_MAX, "heap")), "Heap ceiling"));
        rows.add(timerRow(REQUEST, "Server-side request handling"));
        rows.add(timerRow(RPC, "Server-side handling of clicks and keystrokes"));
        // Client-perceived timings, POSTed back by the in-browser collector.
        // They appear once the browser emits and flushes them — never in a
        // browserless test, which does not run the JS collector.
        rows.add(timerRow(CLIENT_BOOTSTRAP, "App start-up, as the browser saw it"));
        rows.add(timerRow(CLIENT_NAVIGATION, "Client-side navigation"));
        rows.add(timerRow(CLIENT_LCP, "Largest Contentful Paint"));
        rows.add(timerRow(CLIENT_FCP, "First Contentful Paint"));
        rows.add(counterRow(ERRORS, "Server-side failures"));
        rows.add(counterRow(CLIENT_ERRORS, "JavaScript errors in the browser"));
        // The kit's database meter, live, for this route: it climbs as the
        // catalog is refreshed, which is what step 3 explains.
        FetchTotals fetches = fetchTotals();
        rows.add(new MeterTable.Row(DB_FETCH_ROWS, TAG_ROUTE + "=" + ROUTE,
                fetches.present() ? fetches.count() : -1,
                !fetches.present() ? ""
                        : String.format("%.1f rows per fetch",
                                fetches.count() == 0 ? 0
                                        : fetches.rows() / fetches.count()),
                "Result sets read from the database on this page"));
        vitals.setRows(rows);
    }

    /** Step 3: what the last load cost, as the kit saw it. */
    private void refreshCatalogResult() {
        catalogResult.removeAll();
        if (loads.isEmpty()) {
            catalogResult.add(new Span(
                    "Refresh the catalog above. The kit records every JDBC "
                            + "result set into "),
                    Telemetry.chip(DB_FETCH_ROWS),
                    new Span(", tagged with the route it was read for; this "
                            + "view reads that summary right before and after "
                            + "the load."));
            return;
        }
        Load last = loads.get(loads.size() - 1);
        if (!last.monitored()) {
            catalogResult.add(new Span("Loaded "),
                    Telemetry.timing(last.products() + " products"),
                    new Span(", but "), Telemetry.chip(DB_FETCH_ROWS),
                    new Span(" is not present: run a kit build with the "
                            + "database feature and set "
                            + "vaadin.observability.database=true."));
            return;
        }
        catalogResult.add(new Span("Loading "),
                Telemetry.timing(last.products() + " products"),
                new Span(" cost "),
                Telemetry.timing(last.fetches() + " result-set "
                        + (last.fetches() == 1 ? "fetch" : "fetches")),
                new Span(" ("), Telemetry.timing(last.rows() + " rows"),
                new Span(") on "), Telemetry.chip(TAG_ROUTE + "=" + ROUTE),
                new Span(", according to "), Telemetry.chip(DB_FETCH_ROWS),
                new Span(". "));
        if (last.looksLikeNPlusOne()) {
            catalogResult.add(new Span(
                    "That is the N+1: one query for the products, then one "
                            + "more per product for its categories — the eager, "
                            + "unbatched association fetching each collection on "
                            + "its own. Each of them is also a "),
                    Telemetry.chip("vaadin.db.query"),
                    new Span(" span under the request, SQL included."));
        } else {
            catalogResult.add(new Span(
                    "No N+1: the categories came along with the products in "
                            + "the same query."));
        }
    }

    /** Step 4: every load so far, for the before/after. */
    private void refreshLoadHistory() {
        List.copyOf(loadHistory.getBodyRows())
                .forEach(TableRow::removeFromParent);
        if (loads.isEmpty()) {
            TableRow row = new TableRow();
            row.addDataCell("No loads yet.").setColspan(4);
            row.addClassName("order-empty");
            loadHistory.addRows(row);
            return;
        }
        int number = 1;
        for (Load load : loads) {
            TableRow row = loadHistory.addRow();
            row.addDataCell(new Span("#" + number++ + " "), Telemetry.chip(
                    load.joinFetch() ? "join fetch" : "eager, unbatched"));
            row.addDataCell(Integer.toString(load.products()));
            row.addDataCell(Telemetry.timing(
                    load.monitored() ? Long.toString(load.fetches()) : "—"));
            row.addDataCell(
                    load.monitored() ? Long.toString(load.rows()) : "—");
        }
    }

    // ---------- reading the registry ----------

    private MeterTable.Row gaugeRow(String meter, String value, String reads) {
        return new MeterTable.Row(meter, "—", -1, value, reads);
    }

    private MeterTable.Row timerRow(String meter, String reads) {
        Collection<Timer> timers = registry.find(meter).timers();
        long total = 0;
        double sumMs = 0;
        double maxMs = 0;
        for (Timer t : timers) {
            total += t.count();
            sumMs += t.totalTime(TimeUnit.MILLISECONDS);
            maxMs = Math.max(maxMs, t.max(TimeUnit.MILLISECONDS));
        }
        String value = total == 0 ? ""
                : String.format("mean %.1f ms, max %.1f ms", sumMs / total,
                        maxMs);
        return new MeterTable.Row(meter, "—", total, value, reads);
    }

    private MeterTable.Row counterRow(String meter, String reads) {
        Collection<Counter> counters = registry.find(meter).counters();
        double sum = 0;
        for (Counter c : counters) {
            sum += c.count();
        }
        return new MeterTable.Row(meter, "—", -1,
                counters.isEmpty() ? "" : Long.toString(Math.round(sum)),
                reads);
    }

    private double gaugeValue(String meter) {
        Gauge gauge = registry.find(meter).gauge();
        return gauge == null ? Double.NaN : gauge.value();
    }

    private double gaugeSum(String meter, String area) {
        Collection<Gauge> gauges = registry.find(meter).tag("area", area)
                .gauges();
        if (gauges.isEmpty()) {
            return Double.NaN;
        }
        double sum = 0;
        for (Gauge g : gauges) {
            double v = g.value();
            // jvm.memory.max reports -1 for pools with no defined maximum;
            // skip those so an unbounded pool doesn't drag the total negative.
            if (v > 0) {
                sum += v;
            }
        }
        return sum;
    }

    private static String count(double value) {
        return Double.isNaN(value) ? "" : Long.toString(Math.round(value));
    }

    private static String megabytes(double bytes) {
        return Double.isNaN(bytes) ? ""
                : String.format("%.0f MB", bytes / (1024 * 1024));
    }

    /** Aggregate state of this route's {@code vaadin.db.fetch.rows} summary. */
    private record FetchTotals(boolean present, long count, double rows) {
    }

    /**
     * Reads the kit's per-result-set fetch summary for this route. {@code
     * present} is false when the meter has never been registered for it — the
     * running kit build has no database feature, it is disabled, or nothing
     * has been read yet.
     */
    private FetchTotals fetchTotals() {
        Collection<DistributionSummary> summaries = registry.find(DB_FETCH_ROWS)
                .tag(TAG_ROUTE, ROUTE).summaries();
        long count = 0;
        double rows = 0;
        for (DistributionSummary s : summaries) {
            count += s.count();
            rows += s.totalAmount();
        }
        return new FetchTotals(!summaries.isEmpty(), count, rows);
    }
}
