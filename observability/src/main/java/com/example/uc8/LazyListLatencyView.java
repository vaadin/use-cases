package com.example.uc8;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.example.acme.AcmeCatalog;
import com.example.acme.AppWindow;
import com.example.acme.DemoRig;
import com.example.acme.InsightCard;
import com.example.acme.Insights;
import com.example.acme.Investigation;
import com.example.acme.MeterTable;
import com.example.acme.Telemetry;
import com.example.views.MainLayout;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeTable;
import com.vaadin.flow.component.html.NativeTableCell;
import com.vaadin.flow.component.html.NativeTableHeaderCell;
import com.vaadin.flow.component.html.NativeTableRow;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.observability.spring.boot.VaadinObservabilityEndpoint;

/**
 * UC8 — the product search of Acme's order desk is slow: how do you find out
 * why?
 * <p>
 * The view opens with just the story: an {@link AppWindow} showing the screen
 * Acme's clerks take phone orders on — a lazy {@link ComboBox} over the
 * product catalog, backed by a deliberately slow data provider (the latency
 * knob sits in the {@link DemoRig}, not among the observability readouts). The
 * {@link Investigation} below stays hidden until the catalog is first
 * queried, so it appears at the moment the reader has just felt the wait it
 * explains, as collapsible steps taken one at a time.
 * <p>
 * Typing a filter makes the component ask its provider twice: once for the
 * count of matches, once for the visible page. Both queries run
 * <em>after</em> the RPC invocation that triggered them has already returned,
 * while the response is being built, which is why the interaction meters
 * cannot see them: {@code vaadin.rpc.duration} for the keystroke measures
 * microseconds of bookkeeping no matter how slow the backend is. The steps
 * walk that in the order a developer would: <b>2)</b> the interaction timers
 * look innocent, <b>3)</b> the kit's insights endpoint pins the blame — a
 * {@code slow-data-query} finding grouped by (route, component, kind), which
 * is what scales to an application with a hundred views and a thousand lazy
 * components — and <b>4)</b> the raw data query meters as the fleet-wide
 * aggregates UC7's dashboard charts.
 * <p>
 * <b>Scoping.</b> The kit instruments every {@code DataCommunicator},
 * in-memory ones included, and the duration timers carry no route tag, so an
 * unscoped read would average this combo box together with every grid in the
 * application. The rows therefore read the meters by their tags: the timers
 * split by {@code filtered}, which separates a combo box loading matches for
 * typed text ({@code filtered=true}) from any component loading a whole data
 * set ({@code filtered=false}); the two row summaries are scoped to
 * {@code route=orders}. And both tables on this view — the order lines inside
 * the window and the meter readout below it — are plain HTML tables rather
 * than {@code Grid}s, so rendering them does not issue data provider queries
 * on this very route and perturb the numbers the readout displays.
 */
@Route(value = LazyListLatencyView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Slow product search")
@Menu(order = 8, title = "UC8 — Slow product search")
public class LazyListLatencyView extends VerticalLayout {

    /**
     * The route template, which is also the value of the {@code route} tag the
     * kit puts on the row summaries recorded from this view. Named after the
     * Acme screen rather than the use case, so the readout below reads the way
     * it would in a real application. The kit tags by the primary route
     * template, so the {@code uc8} alias keeps the numbered URL working
     * without ever appearing in the telemetry.
     */
    static final String ROUTE = "orders";

    /**
     * The kit reports a data query as slow once it exceeds the insights UX
     * budget (1 s). The default simulated latency sits above it so the verdict
     * appears on the first search; lowering the knob below the budget keeps
     * new searches out of the report.
     */
    static final int DEFAULT_DELAY_MS = 1_200;

    private static final String INSIGHTS_SECTION = "observability";

    private static final String RPC_DURATION = "vaadin.rpc.duration";
    private static final String COUNT_DURATION = "vaadin.data.count.duration";
    private static final String FETCH_DURATION = "vaadin.data.fetch.duration";
    private static final String FETCH_REQUESTED = "vaadin.data.fetch.requested";
    private static final String FETCH_ROWS = "vaadin.data.fetch.rows";

    private static final String TAG_FILTERED = "filtered";
    private static final String TAG_ROUTE = "route";

    private final transient MeterRegistry registry;
    private final transient VaadinObservabilityEndpoint endpoint;
    private final Investigation investigation = new Investigation(
            "Noticed the wait? Every search costs the catalog two queries at "
                    + "the demo rig's latency. Open the steps — the readout "
                    + "updates as you keep ordering.");
    private final MeterTable meters = new MeterTable("Queries");
    private final NativeTable orderLines = new NativeTable();
    private final Paragraph innocentTimers = new Paragraph();
    private final Div verdict = new Div();
    private final IntegerField delay = new IntegerField(
            "Latency per query (ms)");

    public LazyListLatencyView(MeterRegistry registry,
            VaadinObservabilityEndpoint endpoint) {
        this.registry = registry;
        this.endpoint = endpoint;

        add(new H1("UC8 — Why is the product search slow?"));
        add(new Paragraph(
                "Acme Supply's clerks take phone orders on the screen below, "
                        + "and they say finding a product takes forever. Try "
                        + "it yourself — and when you notice the wait, this "
                        + "page will show you how to find out why."));

        add(new H3("1 — Take an order"));
        add(new Paragraph(
                "Search the catalog (Acme sells fasteners, so try \"brass\" "
                        + "or \"hex bolt\"), pick a product and add it to the "
                        + "order. Notice how long each search takes."));

        add(buildOrderDesk());
        add(buildDemoRig());
        add(buildInvestigation());

        investigation.refreshNow();
    }

    // ---------- the Acme order desk and its demo rig ----------

    private AppWindow buildOrderDesk() {
        TextField customer = new TextField("Customer");
        customer.setValue("Root & Branch Garden Centers");
        customer.setReadOnly(true);
        customer.setWidth("22em");

        ComboBox<String> product = new ComboBox<>("Product");
        product.setPageSize(50);
        product.setItems(this::fetch, this::count);
        product.setPlaceholder("Type to search the catalog");
        product.setWidth("22em");

        IntegerField quantity = new IntegerField("Qty");
        quantity.setValue(1);
        quantity.setMin(1);
        quantity.setStepButtonsVisible(true);
        quantity.setWidth("6em");
        quantity.setId("order-quantity");

        NativeTableCell noLinesCell = new NativeTableCell(
                "No lines yet — search a product above.");
        noLinesCell.getElement().setAttribute("colspan", "2");
        NativeTableRow noLines = new NativeTableRow(noLinesCell);
        noLines.addClassName("order-empty");

        Button addLine = new Button("Add to order", event -> {
            String selected = product.getValue();
            Integer qty = quantity.getValue();
            if (selected == null || qty == null) {
                return;
            }
            noLines.removeFromParent();
            orderLines.getBody()
                    .add(new NativeTableRow(new NativeTableCell(selected),
                            new NativeTableCell(qty.toString())));
            product.clear();
            quantity.setValue(1);
            investigation.refreshSoon();
        });

        orderLines.setId("order-lines");
        orderLines.addClassNames("order-lines", "order-lines-numeric");
        orderLines.setWidthFull();
        NativeTableRow header = orderLines.getHead().addRow();
        header.add(new NativeTableHeaderCell("Product"));
        header.add(new NativeTableHeaderCell("Qty"));
        orderLines.getBody().add(noLines);

        return new AppWindow("Acme Supply — Order Desk", ROUTE, customer,
                new HorizontalLayout(Alignment.END, product, quantity,
                        addLine),
                orderLines);
    }

    /** The knob that fakes Acme's slow catalog backend. */
    private DemoRig buildDemoRig() {
        delay.setValue(DEFAULT_DELAY_MS);
        delay.setWidth("14em");
        delay.setStepButtonsVisible(true);
        delay.setMin(0);
        delay.setMax(5_000);
        delay.setId("backend-delay");

        DemoRig rig = new DemoRig(delay);
        rig.setId("simulation-rig");
        return rig;
    }

    // ---------- the investigation, revealed by the first catalog query ------

    private Investigation buildInvestigation() {
        investigation.setId("investigation");
        investigation.onRefresh(this::refreshReadout);

        innocentTimers.setId("innocent-timers");
        investigation.step("2 — The usual suspects look innocent", true,
                innocentTimers);

        verdict.setId("verdict");
        verdict.setWidthFull();
        investigation.step("3 — The kit's verdict", false, new Paragraph(
                "The insights endpoint reports every data query over the UX "
                        + "budget, grouped by route, component and query kind "
                        + "— in an app with a hundred views, this is what "
                        + "names the culprit."),
                verdict);

        Paragraph metersLead = new Paragraph();
        metersLead.add(new Span(
                "The same queries as fleet-wide aggregates — what UC7's "
                        + "Prometheus and Grafana chart. Timers split by "),
                Telemetry.chip(TAG_FILTERED), new Span("; summaries scoped to "),
                Telemetry.chip(TAG_ROUTE + "=" + ROUTE), new Span("."));
        meters.setId("meter-table");
        investigation.step("4 — The raw meters, fleet-wide", false,
                metersLead, meters);

        return investigation;
    }

    // ---------- the deliberately slow provider ----------

    private Stream<String> fetch(Query<String, String> query) {
        investigation.reveal();
        sleep();
        return matches(query.getFilter().orElse("")).stream()
                .skip(query.getOffset()).limit(query.getLimit());
    }

    private int count(Query<String, String> query) {
        investigation.reveal();
        sleep();
        return matches(query.getFilter().orElse("")).size();
    }

    private static List<String> matches(String filter) {
        String needle = filter.toLowerCase(Locale.ROOT);
        return AcmeCatalog.products().stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private void sleep() {
        Integer ms = delay.getValue();
        if (ms == null || ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------- the readout ----------

    private void refreshReadout() {
        refreshInnocentTimers();
        refreshVerdict();
        refreshMeters();
    }

    /**
     * Step 2: the timers a developer would check first. They stay flat however
     * slow the catalog is, because the data queries run after the invocation
     * that triggered them has returned — which is the reason this use case
     * needs step 3 at all.
     */
    private void refreshInnocentTimers() {
        innocentTimers.removeAll();
        long count = 0;
        double totalMs = 0;
        double maxMs = 0;
        for (Timer timer : registry.find(RPC_DURATION).timers()) {
            count += timer.count();
            totalMs += timer.totalTime(TimeUnit.MILLISECONDS);
            maxMs = Math.max(maxMs, timer.max(TimeUnit.MILLISECONDS));
        }
        if (count == 0) {
            innocentTimers.add(new Span(
                    "No interactions recorded yet — use the order desk "
                            + "above."));
            return;
        }
        innocentTimers.add(Telemetry.chip(RPC_DURATION), new Span(
                " — what each click and keystroke costs the server: mean "),
                Telemetry.timing("%.1f ms".formatted(totalMs / count)),
                new Span(", max "),
                Telemetry.timing("%.1f ms".formatted(maxMs)),
                new Span(" over %d invocations. Nothing here explains the "
                        .formatted(count)
                        + "wait: the catalog queries run after the invocation "
                        + "has already returned."));
    }

    /**
     * Step 3: the {@code slow-data-query} (and {@code data-query-error})
     * findings of the kit's insights endpoint, filtered to this route. The
     * same Spring bean serves {@code GET /actuator/vaadin/observability}.
     */
    private void refreshVerdict() {
        verdict.removeAll();
        List<Map<String, Object>> findings = Insights
                .of(endpoint.section(INSIGHTS_SECTION)).stream()
                .filter(insight -> {
                    Object type = insight.get("type");
                    return "slow-data-query".equals(type)
                            || "data-query-error".equals(type);
                })
                .filter(insight -> ROUTE
                        .equals(Insights.evidenceOf(insight).get("route")))
                .toList();
        if (findings.isEmpty()) {
            Paragraph empty = new Paragraph(
                    "No findings yet — the kit reports a data query once it "
                            + "exceeds the 1 s UX budget. Keep ordering; the "
                            + "report updates as you go.");
            empty.addClassName("verdict-empty");
            verdict.add(empty);
            return;
        }
        findings.forEach(insight -> {
            Map<String, Object> evidence = Insights.evidenceOf(insight);
            verdict.add(new InsightCard(insight, List.of(
                    TAG_ROUTE + "=" + Insights.text(evidence.get("route")),
                    Insights.simpleName(
                            Insights.text(evidence.get("component"))),
                    Insights.text(evidence.get("queryKind")) + " query",
                    TAG_FILTERED + "="
                            + Insights.text(evidence.get("filtered")),
                    Insights.text(evidence.get("occurrences")) + "×")));
        });
    }

    /** Step 4: the same queries as meters, read by their tags. */
    private void refreshMeters() {
        meters.setRows(List.of(timerRow(COUNT_DURATION, true,
                "How long counting the matches for the typed text takes"),
                timerRow(COUNT_DURATION, false,
                        "Counts without a filter: components loading a whole "
                                + "data set, on any route"),
                timerRow(FETCH_DURATION, true,
                        "How long loading one page of matches takes"),
                timerRow(FETCH_DURATION, false,
                        "Fetches without a filter: components loading a "
                                + "whole data set, on any route"),
                summaryRow(FETCH_REQUESTED,
                        "Items the product search asked for, on this route "
                                + "only"),
                summaryRow(FETCH_ROWS,
                        "Items the provider returned; a persistent gap "
                                + "against the row above means over-fetching "
                                + "or short pages")));
    }

    private MeterTable.Row timerRow(String name, boolean filtered,
            String reads) {
        long count = 0;
        double totalMs = 0;
        double maxMs = 0;
        for (Timer timer : registry.find(name)
                .tag(TAG_FILTERED, Boolean.toString(filtered)).timers()) {
            count += timer.count();
            totalMs += timer.totalTime(TimeUnit.MILLISECONDS);
            maxMs = Math.max(maxMs, timer.max(TimeUnit.MILLISECONDS));
        }
        String value = count == 0 ? ""
                : "mean %.0f ms, max %.0f ms".formatted(totalMs / count, maxMs);
        return new MeterTable.Row(name, TAG_FILTERED + "=" + filtered, count,
                value, reads);
    }

    private MeterTable.Row summaryRow(String name, String reads) {
        long count = 0;
        double total = 0;
        for (DistributionSummary summary : registry.find(name)
                .tag(TAG_ROUTE, ROUTE).summaries()) {
            count += summary.count();
            total += summary.totalAmount();
        }
        String value = count == 0 ? ""
                : "%.0f items over %d fetches".formatted(total, count);
        return new MeterTable.Row(name, TAG_ROUTE + "=" + ROUTE, count, value,
                reads);
    }
}
