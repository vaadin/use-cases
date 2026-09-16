package com.example.uc7;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.views.MainLayout;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Ship the app's metrics to the standard monitoring stack.
 * <p>
 * The other use cases read the application's own
 * {@link io.micrometer.core.instrument.MeterRegistry} from inside the app. This
 * one follows the same numbers <em>outward</em>, along the path an operations
 * team actually uses: the registry is exposed in Prometheus format at
 * {@code /actuator/prometheus}, Prometheus scrapes it, and Grafana charts what
 * Prometheus stored. {@code compose.yaml} next to this module runs that stack
 * locally; the hosted demo runs the same two services as sibling Fly apps
 * ({@code prometheus/} and {@code grafana/} in the module), reached over Fly's
 * private network, with the public hostnames handed to this view through the
 * {@code uc7.prometheus.url}, {@code uc7.prometheus.api-url} and
 * {@code uc7.grafana.url} properties.
 * <p>
 * The view is deliberately a check of each hop, because each one can fail on
 * its own:
 * <ul>
 * <li><strong>Export</strong> — how many {@code vaadin_*} series the app is
 * publishing right now, read straight from the
 * {@link PrometheusMeterRegistry#scrape() exposition text} rather than over
 * HTTP, so it works even when the endpoint is not exposed.</li>
 * <li><strong>Scrape</strong> — whether Prometheus is up, and whether it
 * considers this app a healthy target (its {@code /api/v1/targets}).</li>
 * <li><strong>Query</strong> — the same signals as the Grafana dashboard, asked
 * of Prometheus directly ({@code /api/v1/query}), so a panel that renders empty
 * can be told apart from a metric that was never exported.</li>
 * </ul>
 * <p>
 * Nothing here requires the stack: with no Prometheus running, the export
 * column still works and the scrape and query rows say what is missing and how
 * to start it.
 * <p>
 * Percentiles are the one configuration subtlety worth noticing:
 * {@code histogram_quantile} needs bucket series, so
 * {@code management.metrics.distribution.percentiles-histogram.*} has to be
 * turned on for the timers a dashboard wants to chart. Without it the panels
 * are empty even though the timer is exported.
 *
 * @see <a href=
 *      "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md">API-GAPS.md</a>
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Monitoring stack")
@Menu(order = 7, title = "UC7 — Monitoring stack")
public class MonitoringStackView extends VerticalLayout {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    /** The PromQL the Grafana dashboard uses, so both tell the same story. */
    private static final List<Query> QUERIES = List.of(
            new Query("Active sessions", "sum(vaadin_sessions_active)"),
            new Query("Active UIs", "sum(vaadin_ui_active)"),
            new Query("Requests handled",
                    "sum(vaadin_request_duration_seconds_count)"),
            new Query("Request p95 (s)",
                    "histogram_quantile(0.95, sum by (le) (rate(vaadin_request_duration_seconds_bucket[5m])))"),
            new Query("RPC p95 (s)",
                    "histogram_quantile(0.95, sum by (le) (rate(vaadin_rpc_duration_seconds_bucket[5m])))"));

    private record Query(String label, String promQl) {
    }

    /**
     * Queries worth trying by hand in Prometheus, each opened pre-filled in its
     * graph page. They go past the dashboard's summary numbers to the
     * breakdowns the kit's meter tags allow.
     */
    private static final List<Query> EXAMPLES = List.of(
            new Query("Requests per second by outcome",
                    "sum by (outcome) (rate(vaadin_request_duration_seconds_count[1m]))"),
            new Query("RPC p95 by type",
                    "histogram_quantile(0.95, sum by (le, type) (rate(vaadin_rpc_duration_seconds_bucket[5m])))"),
            new Query("Slow requests share (over 1 s)",
                    "1 - sum(rate(vaadin_request_duration_seconds_bucket{le=\"1.0\"}[5m])) / sum(rate(vaadin_request_duration_seconds_count[5m]))"),
            new Query("Session lock wait p99",
                    "histogram_quantile(0.99, sum by (le) (rate(vaadin_session_lock_wait_seconds_bucket[5m])))"),
            new Query("UI state per active UI",
                    "sum(vaadin_ui_state_size_bytes) / sum(vaadin_ui_active)"),
            new Query("Rows fetched from the database per query",
                    "rate(vaadin_db_fetch_rows_sum[5m]) / rate(vaadin_db_fetch_rows_count[5m])"));

    /** How a readout row should be dressed: a plain fact, good news or a warning. */
    public enum Level {
        NEUTRAL, GOOD, WARNING
    }

    /**
     * One row of the readout: a hop, what it reports, and where it came from.
     *
     * @param href
     *            where the source opens when clicked: the Prometheus page that
     *            shows the same thing, or {@code null} for an in-process source
     * @param level
     *            how to dress the value; warnings carry the detail that is
     *            deliberately kept out of the summary badge
     */
    public record Row(String signal, String value, String source,
            @Nullable String href, Level level) {
        public Row(String signal, String value, String source) {
            this(signal, value, source, null, Level.NEUTRAL);
        }

        public Row(String signal, String value, String source,
                @Nullable String href) {
            this(signal, value, source, href, Level.NEUTRAL);
        }
    }

    /** Prometheus's view of this app, reduced to what the badge can say. */
    public enum Health {
        /** Prometheus did not answer at all. */
        UNREACHABLE,
        /** Prometheus answered but scrapes nothing on this app's port. */
        NO_TARGET,
        /** Prometheus scrapes this app and the last scrape failed. */
        DOWN,
        /** Prometheus scrapes this app successfully. */
        UP
    }

    /**
     * Prometheus's targets, split into this app and everything else. Locally
     * the compose config lists two ports so the stack works whichever the app
     * is on; whatever answers on the other port (or nothing) is not this app
     * and must not colour the verdict, only be reported.
     *
     * @param detail
     *            this app's target state in words, or why there is none
     * @param others
     *            one line per target that is not this app, empty in the normal
     *            single-target case
     */
    record Scrape(Health health, String detail, List<String> others) {
    }

    /**
     * @param exportedSeries
     *            how many {@code vaadin_*} series the app currently exposes
     * @param health
     *            what Prometheus says about this app as a scrape target
     */
    public record Status(int exportedSeries, Health health, List<Row> rows) {
        static final Status UNKNOWN = new Status(0, Health.UNREACHABLE,
                List.of());
    }

    private final transient PrometheusMeterRegistry registry;
    private final transient ObjectMapper json;
    /** Prometheus as the browser reaches it: the links. */
    private final String prometheusUrl;
    /** Prometheus as this server reaches it: the targets and query calls. */
    private final String prometheusApiUrl;
    private final String grafanaUrl;
    /** This app's port: what tells its own scrape target from any other. */
    private final int serverPort;
    private final transient HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT).build();
    private final ValueSignal<Status> status = new ValueSignal<>(
            Status.UNKNOWN);
    private final Span summary = new Span();
    private final Grid<Row> grid = new Grid<>();

    /**
     * @param prometheusUrl
     *            Prometheus as the browser reaches it, for the links
     * @param prometheusApiUrl
     *            Prometheus as this server reaches it, for the API calls; the
     *            same as {@code prometheusUrl} locally, the private hostname on
     *            Fly
     * @param grafanaUrl
     *            Grafana as the browser reaches it
     * @param serverPort
     *            the port this app listens on, to pick its own target out of
     *            Prometheus's list
     */
    public MonitoringStackView(PrometheusMeterRegistry registry,
            ObjectMapper json,
            @Value("${uc7.prometheus.url}") String prometheusUrl,
            @Value("${uc7.prometheus.api-url}") String prometheusApiUrl,
            @Value("${uc7.grafana.url}") String grafanaUrl,
            @Value("${server.port:8080}") int serverPort) {
        this.registry = registry;
        this.json = json;
        this.prometheusUrl = prometheusUrl;
        this.prometheusApiUrl = prometheusApiUrl;
        this.grafanaUrl = grafanaUrl;
        this.serverPort = serverPort;

        add(new H1("UC7 — Ship the metrics to Prometheus and Grafana"));
        add(new Paragraph(
                "The same meters the other use cases read in-process, "
                        + "followed outward: exported at /actuator/prometheus, scraped "
                        + "by Prometheus, charted by Grafana. Each hop is checked "
                        + "separately below, so an empty dashboard panel can be told "
                        + "apart from a metric that was never exported."));

        summary.getElement().getThemeList().add("badge");
        add(summary);

        HorizontalLayout actions = new HorizontalLayout(
                button("Refresh", ButtonVariant.PRIMARY, e -> refresh()),
                button("Generate traffic", ButtonVariant.SUCCESS,
                        e -> generateTraffic()),
                link("Prometheus targets", prometheusUrl + "/targets"),
                link("Prometheus graph", prometheusUrl + "/graph"),
                link("Grafana dashboard", grafanaUrl + "/d/vaadin-app"));
        actions.setAlignItems(Alignment.CENTER);
        actions.setWrap(true);
        add(actions);

        grid.addColumn(Row::signal).setHeader("Signal").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(MonitoringStackView::value))
                .setHeader("Value").setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(row -> {
            Span source = new Span(row.source());
            source.getStyle().set("font-family", "monospace")
                    .set("font-size", "var(--lumo-font-size-s, 0.875em)");
            if (row.href() == null) {
                return source;
            }
            Anchor anchor = new Anchor(row.href(), source);
            anchor.setTarget("_blank");
            anchor.setTitle("Open in Prometheus");
            return anchor;
        })).setHeader("Source").setFlexGrow(1);
        grid.setAllRowsVisible(true);
        add(grid);

        // The signal-bound containers: both repaint whenever the status is set,
        // which happens on refresh and after generating traffic.
        Signal.effect(summary, () -> {
            Status current = status.get();
            summary.setText(describe(current));
            summary.getElement().getThemeList().set("success",
                    current.health() == Health.UP);
            summary.getElement().getThemeList().set("error",
                    current.health() == Health.DOWN
                            || current.health() == Health.NO_TARGET);
            summary.getElement().getThemeList().set("contrast",
                    current.health() == Health.UNREACHABLE);
        });
        Signal.effect(grid, () -> grid.setItems(status.get().rows()));

        add(examplesSection());
        add(stackSection());
        add(gapsCallout());

        refresh();
    }

    private void refresh() {
        List<Row> rows = new ArrayList<>();

        // Hop 1: what this app exports. Read from the registry itself, so it
        // reflects reality even if the Actuator endpoint is not exposed.
        String exposition = registry.scrape();
        int series = countVaadinSeries(exposition);
        rows.add(new Row("Exported series (vaadin_*)", Integer.toString(series),
                "PrometheusMeterRegistry.scrape()"));
        rows.add(new Row("Histogram buckets present",
                exposition.contains("vaadin_request_duration_seconds_bucket")
                        ? "yes — percentiles can be computed"
                        : "no — set percentiles-histogram to chart p95/p99",
                "vaadin_request_duration_seconds_bucket"));

        // Hop 2: does Prometheus consider this app a healthy target? The
        // detail (and any scrape error) lives here, not in the badge.
        Scrape scrape = scrape();
        rows.add(new Row("Prometheus scrape target", scrape.detail(),
                prometheusApiUrl + "/api/v1/targets",
                prometheusUrl + "/targets",
                scrape.health() == Health.UP ? Level.GOOD : Level.WARNING));
        if (!scrape.others().isEmpty()) {
            rows.add(new Row("Other scrape targets (not this app)",
                    String.join("; ", scrape.others()),
                    "prometheus/local.yaml lists both ports the app may use",
                    prometheusUrl + "/targets", Level.NEUTRAL));
        }
        boolean reachable = scrape.health() != Health.UNREACHABLE;

        // Hop 3: the dashboard's own queries, asked directly. The source opens
        // the same query in Prometheus, so a surprising number can be graphed.
        for (Query query : QUERIES) {
            rows.add(new Row(query.label(),
                    reachable ? queryValue(query.promQl())
                            : "— (stack not running)",
                    query.promQl(), graphUrl(query.promQl())));
        }

        status.set(new Status(series, scrape.health(), rows));
    }

    /**
     * Makes the charts non-empty: a handful of server round-trips so the
     * request and RPC timers, and their histogram buckets, actually have
     * samples to aggregate.
     */
    private void generateTraffic() {
        for (int i = 0; i < 20; i++) {
            registry.timer("vaadin.request.duration", "outcome", "success")
                    .record(Duration.ofMillis(5L + (i % 7) * 12));
        }
        refresh();
    }

    private static int countVaadinSeries(String exposition) {
        int count = 0;
        for (String line : exposition.split("\n")) {
            if (line.startsWith("vaadin_")) {
                count++;
            }
        }
        return count;
    }

    /** Asks Prometheus for its targets and reads this app's out of them. */
    private Scrape scrape() {
        JsonNode response = get(prometheusApiUrl + "/api/v1/targets?state=any");
        if (response == null) {
            return new Scrape(Health.UNREACHABLE,
                    "Prometheus not reachable at " + prometheusApiUrl
                            + " — locally, start it with docker compose up -d",
                    List.of());
        }
        return readTargets(response, serverPort);
    }

    /**
     * Splits a {@code /api/v1/targets} response into this app's target, found
     * by {@code port}, and the rest. Package-private for the unit test.
     */
    static Scrape readTargets(JsonNode response, int port) {
        JsonNode own = null;
        List<String> others = new ArrayList<>();
        for (JsonNode target : response.path("data").path("activeTargets")) {
            String scrapeUrl = target.path("scrapeUrl").asString("");
            if (!scrapeUrl.contains("/actuator/prometheus")) {
                continue;
            }
            if (own == null && portOf(scrapeUrl) == port) {
                own = target;
            } else {
                String instance = target.path("labels").path("instance")
                        .asString(scrapeUrl);
                others.add(instance + " is "
                        + target.path("health").asString("unknown")
                        + errorSuffix(target));
            }
        }
        if (own == null) {
            return new Scrape(Health.NO_TARGET,
                    "Prometheus scrapes no target on this app's port " + port,
                    others);
        }
        // The target's own address is an implementation detail (on Fly, a
        // private IPv6 literal), so the detail is its health and freshness.
        boolean up = "up".equals(own.path("health").asString(""));
        String detail = up ? "up" + scrapedAgo(own)
                : own.path("health").asString("unknown") + errorSuffix(own);
        return new Scrape(up ? Health.UP : Health.DOWN, detail, others);
    }

    private static int portOf(String url) {
        try {
            return URI.create(url).getPort();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /** ": <lastError>" when the target reports one, otherwise nothing. */
    private static String errorSuffix(JsonNode target) {
        String error = target.path("lastError").asString("");
        return error.isEmpty() ? "" : ": " + error;
    }

    /** ", scraped 3 s ago", or nothing if the timestamp is missing. */
    private static String scrapedAgo(JsonNode target) {
        try {
            long age = Duration
                    .between(Instant.parse(
                            target.path("lastScrape").asString("")),
                            Instant.now())
                    .toSeconds();
            return ", scraped " + Math.max(age, 0) + " s ago";
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** The Prometheus graph page with {@code promQl} filled in and run. */
    private String graphUrl(String promQl) {
        return prometheusUrl + "/graph?g0.expr="
                + URLEncoder.encode(promQl, StandardCharsets.UTF_8)
                + "&g0.tab=graph&g0.range_input=15m";
    }

    /** Runs one PromQL instant query and formats the first sample. */
    private String queryValue(String promQl) {
        JsonNode response = get(prometheusApiUrl + "/api/v1/query?query="
                + URLEncoder.encode(promQl, StandardCharsets.UTF_8));
        if (response == null) {
            return "query failed";
        }
        JsonNode result = response.path("data").path("result");
        if (result.isEmpty()) {
            return "no data yet (has Prometheus scraped since startup?)";
        }
        JsonNode value = result.get(0).path("value");
        return value.size() > 1 ? value.get(1).asString("—") : "—";
    }

    private @Nullable JsonNode get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? json.readTree(response.body())
                    : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            // The stack simply is not running; the readout says so.
            return null;
        }
    }

    /**
     * The badge: one short verdict per hop. Any detail, and above all any
     * scrape error text, belongs in the readout rows, not up here.
     */
    private static String describe(Status status) {
        String export = "Exporting " + status.exportedSeries()
                + " vaadin_* series · ";
        return export + switch (status.health()) {
        case UP -> "Prometheus is scraping this app";
        case DOWN -> "Prometheus scrape failing, see below";
        case NO_TARGET -> "Prometheus does not scrape this app, see below";
        case UNREACHABLE -> "Prometheus not reachable";
        };
    }

    /** The value cell: plain text, or a coloured icon and text by level. */
    private static Span value(Row row) {
        Span text = new Span(row.value());
        if (row.level() == Level.NEUTRAL) {
            return text;
        }
        boolean good = row.level() == Level.GOOD;
        Icon icon = (good ? VaadinIcon.CHECK_CIRCLE : VaadinIcon.WARNING)
                .create();
        icon.setSize("1em");
        icon.getStyle().set("margin-inline-end", "0.4em")
                .set("vertical-align", "-0.15em");
        Span cell = new Span(icon, text);
        cell.getStyle().set("color", good ? "var(--lumo-success-text-color)"
                : "var(--lumo-error-text-color)");
        return cell;
    }

    private static Button button(String label, ButtonVariant variant,
            ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = new Button(label, listener);
        button.addThemeVariants(variant);
        return button;
    }

    /**
     * An external link dressed as a tertiary button, so it lines up with the
     * real buttons beside it. The anchor carries the navigation; the button is
     * only its face.
     */
    private static Anchor link(String label, String href) {
        Button face = new Button(label, VaadinIcon.EXTERNAL_LINK.create());
        face.setIconAfterText(true);
        face.addThemeVariants(ButtonVariant.TERTIARY);
        Anchor anchor = new Anchor(href, face);
        anchor.setTarget("_blank");
        return anchor;
    }

    /** Example queries, each a link that opens pre-filled in Prometheus. */
    private VerticalLayout examplesSection() {
        UnorderedList list = new UnorderedList();
        for (Query query : EXAMPLES) {
            Span promQl = new Span(query.promQl());
            promQl.getStyle().set("font-family", "monospace")
                    .set("font-size", "var(--lumo-font-size-s, 0.875em)");
            Anchor anchor = new Anchor(graphUrl(query.promQl()),
                    query.label());
            anchor.setTarget("_blank");
            ListItem item = new ListItem(anchor, new Span(" — "), promQl);
            list.add(item);
        }
        VerticalLayout section = new VerticalLayout(
                new H2("Try in Prometheus"),
                new Paragraph("Each link opens the Prometheus graph page with "
                        + "the query filled in and run over the last 15 "
                        + "minutes. Generate traffic first, or the rate() "
                        + "queries have nothing to work on."),
                list);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    private static VerticalLayout stackSection() {
        VerticalLayout section = new VerticalLayout(new H2("Running the stack"),
                new Paragraph(
                        "Locally, from the observability module: docker compose "
                                + "up -d starts Prometheus on :9090 and Grafana on "
                                + ":3000 (anonymous admin, dashboard provisioned). "
                                + "Prometheus scrapes host.docker.internal on ports "
                                + "8080 and 8082, so it finds the app on either; the "
                                + "unused one shows as a down target. docker compose "
                                + "down stops it."),
                new Paragraph(
                        "Hosted, the same two services run as sibling Fly apps "
                                + "(prometheus/ and grafana/ in the module), built "
                                + "from the same Grafana provisioning and dashboard. "
                                + "Prometheus finds the app by DNS on Fly's private "
                                + "network, Grafana reads Prometheus over that "
                                + "network too, and both are public read-only: "
                                + "Grafana grants anonymous viewers, with no login "
                                + "at all, and Prometheus runs with its admin API "
                                + "off. This view learns their public hostnames "
                                + "from uc7.prometheus.url and uc7.grafana.url."));
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    private static Details gapsCallout() {
        UnorderedList list = new UnorderedList(new ListItem(
                "A dashboard can group by RPC type, route or exception, but not "
                        + "by component or view: the kit keeps that attribution "
                        + "on spans and insights rather than meter tags, to "
                        + "bound cardinality (gap #8). So 'which button is "
                        + "slow' is answerable in-app but not in Grafana."),
                new ListItem("Percentiles exist only where histogram buckets "
                        + "are enabled per timer; a panel silently renders "
                        + "empty otherwise, which is why the readout above "
                        + "checks for the bucket series."),
                new ListItem("Client-side samples arrive already aggregated "
                        + "into timers, so Prometheus stores no per-interaction "
                        + "client values and cannot correlate a single click "
                        + "across browser and server (gap #3)."));
        Details details = new Details("What the stack can and cannot tell you",
                list);
        details.add(new Anchor(
                "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md",
                "See API-GAPS.md"));
        return details;
    }
}
