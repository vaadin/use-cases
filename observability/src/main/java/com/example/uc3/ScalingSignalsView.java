package com.example.uc3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.uc3.HeapCostProbe.HeapCost;
import com.example.views.MainLayout;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.micrometer.ObservabilitySettings;

/**
 * UC3 — Know when to scale.
 * <p>
 * A server-driven app is sized differently from a stateless one. Flow keeps
 * every open tab's component tree in server memory, so "do we need another
 * instance?" is not answered by request rate: it is answered by how much state
 * is being held for live users, and how fast that state is growing.
 * <p>
 * <strong>The kit measures this now.</strong> Earlier revisions of this use
 * case had to build the missing half themselves — walk each UI's state tree,
 * keep a registry of live UIs, publish their aggregate — because the
 * Observability Kit counted sessions and UIs but never their size. That was
 * {@code API-GAPS.md} #6, and the kit has since closed it: turning on
 *
 * <pre>
 * vaadin.observability.ui-state=true
 * </pre>
 *
 * makes every UI report its own state-tree size and publishes
 * {@link MeterNames#UI_STATE_NODES}, {@link MeterNames#UI_STATE_NODES_MAX},
 * {@link MeterNames#UI_STATE_COMPONENTS}, {@link MeterNames#UI_STATE_VIEWS},
 * {@link MeterNames#SESSION_STATE_NODES_MAX},
 * {@link MeterNames#SESSION_UIS_MAX} and
 * {@link MeterNames#UI_STATE_SAMPLE_AGE_MAX}. So this view now does what the
 * rest of the module does — read the application's {@link MeterRegistry} — and
 * the local shim is gone.
 * <p>
 * <strong>What the kit still leaves to the application.</strong> Nodes, not
 * bytes: the kit publishes {@link MeterNames#UI_STATE_SIZE} only once the app
 * says what a node costs, via
 * {@code vaadin.observability.ui-state-bytes-per-node}, because it cannot
 * measure heap per node and will not guess. {@link HeapCostProbe} is that
 * measurement, so the "Measure bytes per node" button below both explains where
 * the configured value came from and checks whether it still holds — a
 * configured constant cannot notice that the views got heavier.
 * <p>
 * <strong>Reading the numbers honestly.</strong> A UI is measured on its own
 * session's thread, so the aggregate contains an idle user's state as of their
 * last interaction, and the sampling of the current tab lags the interaction
 * that changed it — the kit re-measures when an RPC <em>ends</em>, which is
 * after this view's own click handler has already run. That is why
 * {@code vaadin.ui.state.sample.age.max} is in the readout: it is what lets a
 * reading be judged rather than trusted. The "grow this view's state" button
 * shows both things at once — one session and one tab throughout, while the
 * state the server holds for that user jumps on the following refresh.
 * <p>
 * The readout is bound to a {@link ValueSignal} and refreshed on a UI poll, so
 * other users' sessions move the numbers without any interaction here.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Knowing when to add another server")
@Menu(order = 3, title = "UC3 — Knowing when to add another server")
public class ScalingSignalsView extends VerticalLayout {

    private static final String HEAP_USED = "jvm.memory.used";
    private static final String HEAP_MAX = "jvm.memory.max";

    /** How many components the grow button adds per click. */
    static final int GROWTH_STEP = 500;
    private static final int POLL_MILLIS = 2000;

    /** Where a number comes from, which is half of what this view is about. */
    public enum Origin {
        /** Published by the Observability Kit or Spring Boot out of the box. */
        KIT,
        /** Read from the kit's own configuration rather than from a meter. */
        CONFIG,
        /** Measured by this view, because no meter can supply it. */
        MEASURED,
        /** Derived here from the numbers above. */
        DERIVED
    }

    /**
     * One capacity signal.
     *
     * @param why
     *            what this number tells you about needing more capacity — the
     *            column that separates a signal from a statistic
     */
    public record Row(String signal, String value, Origin origin, String source,
            String why) {
    }

    /** The whole readout at a point in time. */
    public record Capacity(String verdict, Pressure pressure, List<Row> rows) {
        static final Capacity EMPTY = new Capacity("—", Pressure.UNKNOWN,
                List.of());
    }

    /** How close the app is to needing more capacity. */
    public enum Pressure {
        /** Heap headroom is not readable, so no claim is made. */
        UNKNOWN,
        /** Plenty of headroom for the state currently held. */
        COMFORTABLE,
        /** Headroom is shrinking; time to plan. */
        WATCH,
        /** Little headroom left for the state being held. */
        TIGHT
    }

    private final transient MeterRegistry registry;
    private final transient ObservabilitySettings settings;
    private final ValueSignal<Capacity> capacity = new ValueSignal<>(
            Capacity.EMPTY);
    private final ValueSignal<HeapCost> heapCost = new ValueSignal<>(
            HeapCost.NONE);
    private final Span verdict = new Span();
    private final Grid<Row> signals = new Grid<>();
    private final Div calibration = new Div();
    /**
     * Where the grow button puts its components. Keeping them in one container
     * makes "reset" a single removeAll and keeps the added state clearly this
     * view's own rather than scattered through the layout.
     */
    private final Div ballast = new Div();
    private @Nullable Registration pollRegistration;

    public ScalingSignalsView(MeterRegistry registry,
            ObservabilitySettings settings) {
        this.registry = registry;
        this.settings = settings;

        add(new H1("UC3 — Know when to scale"));
        add(new Paragraph(
                "Flow holds every open tab's component tree in server memory, "
                        + "so capacity for a server-driven app is a question about "
                        + "state, not about request rate. The Observability Kit "
                        + "publishes both halves: how many sessions and UIs exist, "
                        + "how fast they are created, how long they live and whether "
                        + "their locks are contended — and, with "
                        + "vaadin.observability.ui-state turned on, how much state "
                        + "each of them actually holds. Everything below is read "
                        + "from the application's MeterRegistry."));

        verdict.setId("capacity-verdict");
        verdict.getElement().getThemeList().add("badge");
        add(verdict);

        add(new HorizontalLayout(
                button("Refresh", "refresh", ButtonVariant.PRIMARY,
                        this::recompute),
                button("Grow this view's state (+" + GROWTH_STEP
                        + " components)", "grow-state", ButtonVariant.SUCCESS,
                        this::growState),
                button("Reset added state", "reset-state",
                        ButtonVariant.TERTIARY, this::resetState),
                button("Measure bytes per node", "measure-cost",
                        ButtonVariant.PRIMARY, this::measureCost)));

        add(configSection());
        add(signalsSection());
        add(calibrationSection());
        // Collapsed, not detached: a Details keeps its content in the server
        // tree whether or not it is open, so the added state is real while the
        // page stays readable. That is itself the lesson — what the user cannot
        // see still costs memory.
        Details added = new Details("Components added by the grow button",
                ballast);
        added.setId("ballast-section");
        add(added);
        add(gapsCallout());

        recompute();
    }

    /**
     * The kit's UI-state feature is opt-in and off by default, because it costs
     * a tree walk that ordinary request handling does not. A readout of numbers
     * that are simply absent would be baffling, so the configuration it depends
     * on is stated up front, read from the kit's own settings rather than from
     * a copy of the properties.
     */
    private VerticalLayout configSection() {
        Div config = new Div();
        config.setId("ui-state-config");
        config.setText(describeSettings());

        VerticalLayout section = new VerticalLayout(
                new H2("What the kit is configured to measure"), config);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    private String describeSettings() {
        if (!settings.isUiState()) {
            return "vaadin.observability.ui-state is off, so the kit publishes "
                    + "session and UI counts but no state sizes, and the size "
                    + "rows below are empty. Set "
                    + "vaadin.observability.ui-state=true to turn the "
                    + "measurement on; it is off by default because it costs a "
                    + "state-tree walk per sampled interaction.";
        }
        String bytes = settings.getUiStateBytesPerNode() > 0
                ? settings.getUiStateBytesPerNode() + " bytes per node"
                : "no bytes-per-node configured, so " + MeterNames.UI_STATE_SIZE
                        + " is not published at all";
        return String.format(
                "vaadin.observability.ui-state is on: each UI is re-measured at "
                        + "most once per %d ms (ui-state-sample-interval), and "
                        + "the byte projection uses %s "
                        + "(ui-state-bytes-per-node).",
                settings.getUiStateSampleInterval(), bytes);
    }

    private VerticalLayout signalsSection() {
        signals.setId("capacity-grid");
        // The "why" column carries a sentence or two per row, and a Grid clips
        // cell content by default, so the explanation — the point of this
        // readout — would be cut off with no way to expand it. Wrapping lets
        // the rows grow to fit instead.
        //
        // The columns then get fixed widths rather than auto width: several of
        // these cells hold long meter names, and sizing them to their content
        // would push the prose off the right edge.
        signals.addThemeVariants(GridVariant.WRAP_CELL_CONTENT);
        signals.addColumn(Row::signal).setHeader("Signal").setWidth("13em")
                .setFlexGrow(0);
        signals.addColumn(Row::value).setHeader("Value").setWidth("12em")
                .setFlexGrow(0);
        signals.addColumn(row -> switch (row.origin()) {
        case KIT -> "kit meter";
        case CONFIG -> "kit config";
        case MEASURED -> "measured here";
        case DERIVED -> "derived";
        }).setHeader("Source of truth").setWidth("9em").setFlexGrow(0);
        // A meter name is a single unbroken token, so wrapping at spaces cannot
        // help it; this column renders in a span that may break anywhere.
        signals.addColumn(
                new ComponentRenderer<>(row -> breakable(row.source())))
                .setHeader("Source").setWidth("15em").setFlexGrow(0);
        signals.addColumn(Row::why).setHeader("Why it predicts capacity")
                .setWidth("18em").setFlexGrow(1);
        signals.setAllRowsVisible(true);

        // The primary signal-bound containers: both re-run whenever the
        // snapshot is set, which happens on every poll tick and after every
        // button.
        Signal.effect(verdict, () -> {
            Capacity current = capacity.get();
            verdict.setText(current.verdict());
            ThemeList themes = verdict.getElement().getThemeList();
            themes.set("success", current.pressure() == Pressure.COMFORTABLE);
            themes.set("warning", current.pressure() == Pressure.WATCH);
            themes.set("error", current.pressure() == Pressure.TIGHT);
        });
        Signal.effect(signals, () -> signals.setItems(capacity.get().rows()));

        VerticalLayout section = new VerticalLayout(
                new H2("The signals that predict capacity"), signals);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    private VerticalLayout calibrationSection() {
        calibration.setId("calibration-result");
        Signal.effect(calibration,
                () -> calibration.setText(describe(heapCost.get())));

        VerticalLayout section = new VerticalLayout(
                new H2("From nodes to bytes"),
                new Paragraph(
                        "The kit counts nodes and stops there on purpose: a node "
                                + "count is the shape of the state, not its weight, "
                                + "since one grid node backed by 100 000 rows counts "
                                + "as one node. It publishes "
                                + MeterNames.UI_STATE_SIZE
                                + " only once the application supplies the "
                                + "conversion, and the conversion has to be measured "
                                + "— which is what this button does: "
                                + HeapCostProbe.DEFAULT_INSTANCES
                                + " copies of a representative view (a form, a "
                                + "data-bound grid, an action bar) are built and kept "
                                + "reachable while the heap is read before and after — "
                                + "three times over, median wins, since one reading is "
                                + "too noisy to configure from. The result is the value "
                                + "to set as "
                                + "vaadin.observability.ui-state-bytes-per-node, and "
                                + "re-running it is how you find out that the "
                                + "configured one has gone stale."),
                calibration);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        // Poll so the readout is genuinely live: other sessions arriving and
        // leaving, and their state growing, show up without a click. It is also
        // what surfaces this tab's own growth, since the kit re-measures a UI
        // when an interaction ends — after this view's handler has run.
        //
        // setPollInterval and the listener live on the UI, which outlives this
        // view, so both are undone in onDetach — otherwise the UI would keep
        // polling and keep invoking recompute() on a detached view after
        // navigating away (also pinning this view in memory). Polling is
        // assumed
        // disabled elsewhere, so onDetach simply turns it back off.
        UI ui = event.getUI();
        ui.setPollInterval(POLL_MILLIS);
        pollRegistration = ui.addPollListener(poll -> recompute());
        recompute();
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

    /**
     * Adds a batch of components to this view. Nothing about the session or UI
     * <em>count</em> changes — one user, one tab, before and after — while the
     * state the server holds for that user jumps. That gap is exactly what
     * makes counts a poor scaling signal.
     */
    private void growState() {
        for (int i = 0; i < GROWTH_STEP; i++) {
            ballast.add(new Span("ballast " + i));
        }
        recompute();
    }

    private void resetState() {
        ballast.removeAll();
        recompute();
    }

    /**
     * Runs the byte-cost measurement. It blocks for a moment (two collection
     * requests and the batch construction), which is honest about what it is: a
     * measurement, not a meter.
     */
    private void measureCost() {
        heapCost.set(HeapCostProbe.measure(HeapCostProbe.DEFAULT_INSTANCES));
        recompute();
    }

    /** Rebuilds the readout from the registry. */
    private void recompute() {
        // peek(), not get(): this runs from a poll listener and from click
        // handlers, not inside an effect, so reading the signal must not try to
        // set up dependency tracking.
        HeapCost cost = heapCost.peek();
        double nodes = gaugeValue(MeterNames.UI_STATE_NODES);
        double uis = gaugeValue(MeterNames.UI_ACTIVE);
        double heapUsed = gaugeSum(HEAP_USED, "heap");
        double heapMax = gaugeSum(HEAP_MAX, "heap");
        double headroom = heapMax - heapUsed;
        int bytesPerNode = bytesPerNode(cost);

        List<Row> rows = new ArrayList<>();

        rows.add(new Row("Active sessions",
                count(gaugeValue(MeterNames.SESSIONS_ACTIVE)), Origin.KIT,
                MeterNames.SESSIONS_ACTIVE,
                "Live users, each with server-side state. Sets the floor on "
                        + "memory, but says nothing about what each one costs."));
        rows.add(new Row("Active UIs (browser tabs)", count(uis), Origin.KIT,
                MeterNames.UI_ACTIVE,
                "Every tab is a separate component tree. UIs, not sessions, "
                        + "are what multiply retained state."));
        rows.add(new Row("Sessions created",
                count(counterTotal(MeterNames.SESSIONS_CREATED)), Origin.KIT,
                MeterNames.SESSIONS_CREATED,
                "Arrival rate. Multiplied by session lifetime it gives the "
                        + "concurrency a deployment has to hold (Little's law) — "
                        + "the number to size for, rather than today's peak."));
        rows.add(timing("Session lifetime", MeterNames.SESSIONS_DURATION,
                Origin.KIT,
                "How long each user's state is retained. Long-lived sessions "
                        + "and a short timeout are opposite ends of the same "
                        + "capacity dial."));
        rows.add(new Row("UIs created",
                count(counterTotal(MeterNames.UI_CREATED)), Origin.KIT,
                MeterNames.UI_CREATED,
                "Tab churn. A rate far above session creation means users "
                        + "open many tabs per visit, each with its own tree."));
        rows.add(new Row("Most UIs one session holds",
                count(gaugeValue(MeterNames.SESSION_UIS_MAX)), Origin.KIT,
                MeterNames.SESSION_UIS_MAX,
                "One tab-hoarding user costs several trees. Sizing on session "
                        + "count alone underestimates memory by this factor."));

        rows.add(new Row("UI state nodes (total)", count(nodes), Origin.KIT,
                MeterNames.UI_STATE_NODES,
                "The scaling signal: all UI state the server is currently "
                        + "holding. Grows both with more users and with what each "
                        + "of them opened — the only one of these numbers that "
                        + "tracks actual memory pressure."));
        rows.add(new Row("State nodes per UI (mean)",
                Double.isNaN(nodes) || Double.isNaN(uis) || uis <= 0 ? "—"
                        : String.format("%.0f", nodes / uis),
                Origin.DERIVED,
                MeterNames.UI_STATE_NODES + " / " + MeterNames.UI_ACTIVE,
                "Per-user cost. Multiply by expected concurrency to size an "
                        + "instance; watch it climb when a heavy view ships."));
        rows.add(new Row("Largest single UI",
                count(gaugeValue(MeterNames.UI_STATE_NODES_MAX)), Origin.KIT,
                MeterNames.UI_STATE_NODES_MAX,
                "Worst case for one tab. The tail is what exhausts a heap, "
                        + "not the mean."));
        rows.add(new Row("Largest single session",
                count(gaugeValue(MeterNames.SESSION_STATE_NODES_MAX)),
                Origin.KIT, MeterNames.SESSION_STATE_NODES_MAX,
                "Worst case for one user, across all their tabs."));
        rows.add(new Row("Components retained",
                count(gaugeValue(MeterNames.UI_STATE_COMPONENTS)), Origin.KIT,
                MeterNames.UI_STATE_COMPONENTS,
                "Java objects held for live users, as opposed to nodes: the "
                        + "listeners and bindings attached to them are what make "
                        + "a tree expensive to keep."));
        rows.add(new Row("Retained view instances",
                count(gaugeValue(MeterNames.UI_STATE_VIEWS)), Origin.KIT,
                MeterNames.UI_STATE_VIEWS,
                "Route targets still in a tree. More than one per UI means "
                        + "views outlive their navigation — a leak that scales "
                        + "with every user."));
        rows.add(new Row("Stalest measurement",
                seconds(gaugeValue(MeterNames.UI_STATE_SAMPLE_AGE_MAX)),
                Origin.KIT, MeterNames.UI_STATE_SAMPLE_AGE_MAX,
                "How much to trust the rows above. A UI is measured on its own "
                        + "session's thread, so an idle user contributes their "
                        + "state as of their last interaction — and this tab's "
                        + "own growth lands one refresh late."));

        rows.add(new Row("Heap used", megabytes(heapUsed), Origin.KIT,
                HEAP_USED + " {area=heap}",
                "The ceiling everything above is spent against."));
        rows.add(new Row("Heap max", megabytes(heapMax), Origin.KIT,
                HEAP_MAX + " {area=heap}",
                "What one instance can hold. Scaling a server-driven app is "
                        + "mostly about this number against the state above."));
        rows.add(new Row("Heap headroom", headroomText(heapUsed, heapMax),
                Origin.DERIVED, HEAP_MAX + " − " + HEAP_USED,
                "What is left for new users. Read together with the per-user "
                        + "cost below, this is the scaling decision."));

        rows.add(new Row("Configured bytes per node",
                settings.getUiStateBytesPerNode() > 0
                        ? settings.getUiStateBytesPerNode() + " B"
                        : "not configured",
                Origin.CONFIG, "vaadin.observability.ui-state-bytes-per-node",
                "What the kit multiplies the node count by. It cannot measure "
                        + "this, so it publishes no byte figure until told — a "
                        + "guessed per-user cost would be worse than none."));
        rows.add(new Row("Retained UI state (bytes)",
                bytes(gaugeValue(MeterNames.UI_STATE_SIZE)), Origin.KIT,
                MeterNames.UI_STATE_SIZE,
                "Node count times the configured cost per node: what the "
                        + "server is spending on live users right now. Absent "
                        + "until bytes-per-node is set."));
        rows.add(new Row("Measured bytes per node",
                cost.isMeasured() ? cost.bytesPerNode() + " B"
                        : "not measured yet",
                Origin.MEASURED, "HeapCostProbe (no kit meter can supply it)",
                "What a node actually costs in this build. Measure it to get "
                        + "the value to configure — and re-measure to catch a "
                        + "configured constant that the views have outgrown."));
        rows.add(new Row("Room for more UIs",
                projection(headroom, nodes, uis, bytesPerNode), Origin.DERIVED,
                "heap headroom ÷ (nodes per UI × bytes per node)",
                "The scaling answer: how many more tabs fit before this "
                        + "instance is full. Falling as users arrive is the cue "
                        + "to add capacity."));

        rows.add(timing("Session lock wait", MeterNames.SESSION_LOCK_WAIT,
                Origin.KIT,
                "Contention on the per-session lock: requests of one user "
                        + "queueing behind each other. Rising waits mean "
                        + "saturation — CPU or slow handlers — which no memory "
                        + "number reveals."));
        rows.add(timing("Server request handling", MeterNames.REQUEST_DURATION,
                Origin.KIT,
                "Throughput and latency. Latency climbing while state is flat "
                        + "points at CPU, not heap — a different kind of scaling."));

        capacity.set(new Capacity(verdictText(nodes, heapUsed, heapMax),
                pressure(heapUsed, heapMax), rows));
    }

    /**
     * The byte cost to project with: a fresh measurement if one has been taken,
     * otherwise whatever the kit is configured with, and zero when neither
     * exists — in which case the projection says so instead of inventing one.
     */
    private int bytesPerNode(HeapCost cost) {
        return cost.isMeasured() ? cost.bytesPerNode()
                : settings.getUiStateBytesPerNode();
    }

    /**
     * The headline. It leads with state rather than with user count, because
     * that is the argument this view makes, and it says when the measurement is
     * simply switched off rather than showing zeros.
     */
    private String verdictText(double nodes, double heapUsed, double heapMax) {
        StringBuilder text = new StringBuilder();
        if (Double.isNaN(nodes)) {
            text.append("UI state size is not being measured (set "
                    + "vaadin.observability.ui-state=true)");
        } else {
            text.append(String.format(
                    "%s state nodes held for %s UI(s) in %s session(s)",
                    count(nodes), count(gaugeValue(MeterNames.UI_ACTIVE)),
                    count(gaugeValue(MeterNames.SESSIONS_ACTIVE))));
            double size = gaugeValue(MeterNames.UI_STATE_SIZE);
            if (!Double.isNaN(size)) {
                text.append(" ≈ ").append(bytes(size));
            }
        }
        if (!Double.isNaN(heapUsed) && !Double.isNaN(heapMax) && heapMax > 0) {
            text.append(String.format("; heap %s of %s (%.0f %% used)",
                    megabytes(heapUsed), megabytes(heapMax),
                    100 * heapUsed / heapMax));
        }
        return text.toString();
    }

    /**
     * Heap pressure, derived rather than hardcoded so the badge never claims a
     * state it cannot see: unknown when the JVM gauges are absent, and
     * otherwise banded by how much of the heap is already spent.
     */
    static Pressure pressure(double heapUsed, double heapMax) {
        if (Double.isNaN(heapUsed) || Double.isNaN(heapMax) || heapMax <= 0) {
            return Pressure.UNKNOWN;
        }
        double usedFraction = heapUsed / heapMax;
        if (usedFraction < 0.6) {
            return Pressure.COMFORTABLE;
        }
        return usedFraction < 0.85 ? Pressure.WATCH : Pressure.TIGHT;
    }

    /**
     * How many more UIs fit in the remaining heap at this per-node cost. Every
     * branch that cannot answer says why instead of printing a number that
     * would be invented.
     */
    static String projection(double headroom, double nodes, double uis,
            int bytesPerNode) {
        if (bytesPerNode <= 0) {
            return "measure or configure bytes per node";
        }
        if (Double.isNaN(headroom)) {
            return "heap gauges unavailable";
        }
        if (Double.isNaN(nodes) || Double.isNaN(uis) || uis <= 0
                || nodes <= 0) {
            return "no UI state measured yet";
        }
        double bytesPerUi = nodes / uis * bytesPerNode;
        return String.format("≈ %,d more UIs (%s each)",
                Math.max(0, (long) (headroom / bytesPerUi)), bytes(bytesPerUi));
    }

    private String describe(HeapCost cost) {
        if (!cost.isMeasured()) {
            return "Not measured yet — click \"Measure bytes per node\". The "
                    + "measurement pauses the server for a second or so: it "
                    + "builds and weighs the batch three times and takes the "
                    + "median, because a single heap reading is too noisy to "
                    + "put in a configuration file.";
        }
        int configured = settings.getUiStateBytesPerNode();
        String drift;
        if (configured <= 0) {
            drift = "Nothing is configured yet, so " + MeterNames.UI_STATE_SIZE
                    + " is not published; set "
                    + "vaadin.observability.ui-state-bytes-per-node="
                    + cost.bytesPerNode() + " to turn it on.";
        } else if (configured == cost.bytesPerNode()) {
            drift = "That matches the configured "
                    + "ui-state-bytes-per-node exactly.";
        } else {
            drift = String.format(
                    "The kit is configured with %d B per node, so its byte "
                            + "figure is off by about %.0f %% for a view of this "
                            + "shape — a configured constant cannot notice that "
                            + "the views changed.",
                    configured,
                    100.0 * Math.abs(configured - cost.bytesPerNode())
                            / cost.bytesPerNode());
        }
        return String.format(
                "%d representative views retained %s (median of three "
                        + "rounds): %s per view instance, %d bytes per state "
                        + "node (%d nodes each). %s "
                        + "System.gc() is a hint, not a command, so treat this "
                        + "as a floor on per-user cost: it covers the component "
                        + "tree and its data, not session attributes, security "
                        + "context or JPA caches.",
                cost.instances(), bytes(cost.heapDeltaBytes()),
                bytes(cost.bytesPerInstance()), cost.bytesPerNode(),
                cost.nodesPerInstance(), drift);
    }

    private Row timing(String label, String meter, Origin origin, String why) {
        Collection<Timer> timers = registry.find(meter).timers();
        long total = 0;
        double sumMs = 0;
        double maxMs = 0;
        for (Timer timer : timers) {
            total += timer.count();
            sumMs += timer.totalTime(TimeUnit.MILLISECONDS);
            maxMs = Math.max(maxMs, timer.max(TimeUnit.MILLISECONDS));
        }
        String value = total == 0 ? "no samples yet"
                : String.format("mean %.1f ms, max %.1f ms (%d)", sumMs / total,
                        maxMs, total);
        return new Row(label, value, origin, meter, why);
    }

    /**
     * {@code NaN} when the meter is absent, which the formatters render as an
     * em dash.
     */
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
        for (Gauge gauge : gauges) {
            double value = gauge.value();
            // jvm.memory.max reports -1 for pools with no defined maximum; skip
            // those so an unbounded pool doesn't drag the total negative.
            if (value > 0) {
                sum += value;
            }
        }
        return sum;
    }

    private double counterTotal(String meter) {
        Collection<Counter> counters = registry.find(meter).counters();
        double sum = 0;
        for (Counter counter : counters) {
            sum += counter.count();
        }
        return sum;
    }

    private static String headroomText(double heapUsed, double heapMax) {
        if (Double.isNaN(heapUsed) || Double.isNaN(heapMax) || heapMax <= 0) {
            return "—";
        }
        return String.format("%s (%.0f %%)", megabytes(heapMax - heapUsed),
                100 * (heapMax - heapUsed) / heapMax);
    }

    private static String count(double value) {
        return Double.isNaN(value) ? "—" : Long.toString(Math.round(value));
    }

    private static String seconds(double value) {
        return Double.isNaN(value) ? "—" : String.format("%.1f s ago", value);
    }

    private static String megabytes(double byteCount) {
        return Double.isNaN(byteCount) ? "—"
                : String.format("%.0f MB", byteCount / (1024 * 1024));
    }

    private static String bytes(double byteCount) {
        if (Double.isNaN(byteCount)) {
            return "—";
        }
        if (byteCount >= 1024 * 1024) {
            return String.format("%.1f MB", byteCount / (1024d * 1024));
        }
        return byteCount >= 1024 ? String.format("%.1f kB", byteCount / 1024d)
                : Math.round(byteCount) + " B";
    }

    /**
     * Renders a value that has to be allowed to break mid-token.
     * <p>
     * Wrapping a grid cell only introduces line breaks at spaces, and a meter
     * name is a single unbroken token — {@code vaadin.ui.state.sample.age.max}
     * has nowhere to wrap — so the cell falls back to clipping it with an
     * ellipsis. {@code overflow-wrap: anywhere} lets the name wrap onto a
     * second line instead, which matters here because naming the source meter
     * is half of what this readout is for.
     */
    static Span breakable(String text) {
        Span span = new Span(text);
        span.getStyle().set("overflow-wrap", "anywhere").set("white-space",
                "normal");
        return span;
    }

    private static Button button(String label, String id, ButtonVariant variant,
            Runnable action) {
        Button button = new Button(label, event -> action.run());
        button.setId(id);
        button.addThemeVariants(variant);
        return button;
    }

    private static Details gapsCallout() {
        UnorderedList list = new UnorderedList(new ListItem(
                "Per-UI state size used to be missing (gap #6) and is now a kit "
                        + "feature: vaadin.observability.ui-state=true makes each "
                        + "UI measure its own state tree and publishes the "
                        + "aggregate. This view used to carry that code; it now "
                        + "just reads the meters."),
                new ListItem("The gauges are aggregates only — totals and "
                        + "maxima — so \"which user is holding all that state\" "
                        + "cannot be answered from inside the app. The kit knows "
                        + "per-UI figures but keeps them private, and rightly "
                        + "does not tag meters per session, since that would "
                        + "grow unbounded. A read-only per-session accessor "
                        + "would make an in-app breakdown possible without "
                        + "touching cardinality."),
                new ListItem("Bytes are configured, not measured. The kit will "
                        + "not guess what a node costs, which is right, but it "
                        + "leaves the application to measure it out of band and "
                        + "hard-code the result — and a configured constant "
                        + "cannot notice that the views got heavier. The button "
                        + "above is that measurement; nothing reconciles the two "
                        + "automatically."),
                new ListItem("There is no way to ask for a measurement. "
                        + "Sampling happens at UI init, after navigation, and "
                        + "when an RPC ends — so a view that wants to show what "
                        + "this tab costs right now has to wait for the next "
                        + "interaction, which is why the numbers here land one "
                        + "refresh behind the grow button."),
                new ListItem("Measurement is opt-in and costs a tree walk, so "
                        + "it is off by default; and an idle user's state is "
                        + "only as current as their last interaction. "
                        + "vaadin.ui.state.sample.age.max is what makes that "
                        + "staleness visible rather than silent."));
        Details details = new Details("What this can't show yet (and why)",
                list);
        details.add(new Anchor(
                "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md",
                "See API-GAPS.md"));
        return details;
    }
}
