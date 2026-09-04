package com.example.uc3;

import java.util.List;

import com.example.home.HomeView;
import com.example.uc3.ScalingSignalsView.Origin;
import com.example.uc3.ScalingSignalsView.Pressure;
import com.example.uc3.ScalingSignalsView.Row;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.micrometer.ObservabilitySettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC3 no longer measures anything itself: the Observability Kit's
 * {@code ui-state} feature publishes the state sizes, so these tests check that
 * the view reads the kit's meters, states the configuration it depends on, and
 * degrades honestly where the kit stops — at bytes, which it will not guess.
 * <p>
 * The kit gauges are asserted against the {@link MeterRegistry} and the
 * Prometheus exposition rather than only through the grid, because those are
 * what UC7's dashboard reads.
 */
@SpringBootTest
@ViewPackages(classes = { ScalingSignalsView.class, HomeView.class })
// A fresh context per test. The kit's binder is created by the Vaadin service
// init listener and registers gauges that hold it; re-initialising the Vaadin
// environment between tests builds a second binder while the shared registry
// still serves the first one's gauges, which then report zero — or NaN, once
// that binder is collected out of the weak reference the gauge holds it by —
// because the UIs they knew about are gone. Reloading the context keeps
// registry and binder in step, so a gauge reading means what it says.
//
// Dirtied *before* each method rather than after, because the hazard is the
// context this class is handed, not the one it leaves behind: the first method
// inherits whatever context an earlier @SpringBootTest class already built and
// initialised a Vaadin environment against, and the binder behind that
// registry's gauges is dead before this class starts. Dirtying afterwards left
// exactly that one method unprotected, so whichever test ran first read a dead
// gauge and failed depending on the order the classes happened to run in.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ScalingSignalsViewTest extends SpringBrowserlessTest {

    @Autowired
    MeterRegistry registry;

    @Autowired
    PrometheusMeterRegistry prometheus;

    @Autowired
    ObservabilitySettings settings;

    @Test
    void moduleTurnsOnTheKitsUiStateMeasurement() {
        // Everything else here depends on this: the feature is off by default,
        // so if the module's properties ever lose it, UC3 has nothing to show
        // and should fail here rather than render a page of em dashes.
        assertTrue(settings.isUiState(),
                "the module should enable vaadin.observability.ui-state");
        assertTrue(settings.getUiStateBytesPerNode() > 0,
                "a measured bytes-per-node should be configured so "
                        + MeterNames.UI_STATE_SIZE + " is published");
    }

    @Test
    void rendersHeadingVerdictActionsAndReadout() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(H1.class).all().stream()
                        .anyMatch(h -> h.getText().startsWith("UC3")),
                "UC3 heading should render");

        Span verdict = findInView(Span.class).id("capacity-verdict");
        assertTrue(verdict.getText().contains("state nodes"),
                "the badge should lead with the state being held: "
                        + verdict.getText());

        for (String id : new String[] { "refresh", "grow-state", "reset-state",
                "measure-cost" }) {
            assertNotNull(findInView(Button.class).id(id),
                    "action button should render: " + id);
        }
        assertTrue(capacityRows().size() >= 18,
                "the readout should list the whole capacity signal set");
    }

    @Test
    void readsTheKitsMetersRatherThanMeasuringItself() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        // The state-size rows are the ones UC3 used to compute itself; they
        // must
        // now be attributed to the kit's own meters, which is the whole point
        // of
        // this revision.
        for (String meter : new String[] { MeterNames.UI_STATE_NODES,
                MeterNames.UI_STATE_NODES_MAX, MeterNames.UI_STATE_COMPONENTS,
                MeterNames.UI_STATE_VIEWS, MeterNames.SESSION_STATE_NODES_MAX,
                MeterNames.SESSION_UIS_MAX,
                MeterNames.UI_STATE_SAMPLE_AGE_MAX }) {
            assertTrue(
                    capacityRows().stream()
                            .anyMatch(row -> row.origin() == Origin.KIT
                                    && meter.equals(row.source())),
                    "the readout should attribute this to the kit: " + meter);
        }
        // No uc3.* gauge should be left behind from when this view published
        // its
        // own.
        assertTrue(registry.find("uc3.ui.state.nodes").gauge() == null,
                "the local shim's gauges should be gone");
    }

    @Test
    void theKitPublishesTheStateSizeGauges() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        Gauge nodes = registry.find(MeterNames.UI_STATE_NODES).gauge();
        assertNotNull(nodes, MeterNames.UI_STATE_NODES
                + " should be registered once ui-state is on");
        assertTrue(nodes.value() > 0,
                "an attached view should account for some state, got "
                        + nodes.value());

        Gauge views = registry.find(MeterNames.UI_STATE_VIEWS).gauge();
        assertNotNull(views, "the retained-view gauge should be registered");
        assertTrue(views.value() >= 1,
                "the attached UC3 view is itself a retained route target, got "
                        + views.value());

        // Byte figure only exists because the module configured a cost.
        assertNotNull(registry.find(MeterNames.UI_STATE_SIZE).gauge(),
                MeterNames.UI_STATE_SIZE
                        + " should be published when bytes-per-node is set");
    }

    @Test
    void exportsTheCapacityGaugesUnderTheNamesTheDashboardQueries() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        // UC7's Grafana dashboard charts these by name, and Micrometer's
        // Prometheus convention rewrites names on the way out, so pinning the
        // exposition names is the only thing that catches a rename or a stray
        // base unit turning vaadin.ui.state.views into
        // vaadin_ui_state_views_views.
        String exposition = prometheus.scrape();
        for (String series : new String[] { "vaadin_ui_state_nodes",
                "vaadin_ui_state_nodes_max", "vaadin_ui_state_components",
                "vaadin_ui_state_views", "vaadin_ui_state_size_bytes",
                "vaadin_ui_state_sample_age_max_seconds",
                "vaadin_session_state_nodes_max", "vaadin_session_uis_max" }) {
            assertTrue(exposition.contains("\n" + series + " "),
                    "the exposition should carry " + series
                            + " as its own series");
        }
    }

    @Test
    void reportsTheKitConfigurationTheReadoutDependsOn() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        // The feature is opt-in, so a reader has to be able to tell "nothing is
        // happening" from "nothing is measured".
        String config = findInView(Div.class).id("ui-state-config").getText();
        assertTrue(config.contains("ui-state is on"),
                "the config readout should confirm the feature is on: "
                        + config);
        assertTrue(
                config.contains(
                        String.valueOf(settings.getUiStateSampleInterval())),
                "the config readout should name the sample interval: "
                        + config);
        assertTrue(
                config.contains(
                        String.valueOf(settings.getUiStateBytesPerNode())),
                "the config readout should name the configured cost: "
                        + config);

        Row configured = capacityRow("Configured bytes per node");
        assertEquals(Origin.CONFIG, configured.origin(),
                "the byte cost comes from configuration, not from a meter");
    }

    @Test
    void theKitsNodeGaugeTracksTheSizeOfTheTree() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        double nodesBefore = gauge(MeterNames.UI_STATE_NODES);
        double uisBefore = gauge(MeterNames.UI_ACTIVE);
        double sessionsBefore = gauge(MeterNames.SESSIONS_ACTIVE);

        // Grow this tab's tree, then navigate: a real navigation is one of the
        // points at which the kit re-measures, so this exercises end to end the
        // meter UC3's whole argument rests on. The ballast hangs off the UI
        // rather than the view so that it survives leaving the view behind.
        UI ui = UI.getCurrent();
        Div ballast = new Div();
        for (int i = 0; i < ScalingSignalsView.GROWTH_STEP; i++) {
            ballast.add(new Span("ballast " + i));
        }
        ui.add(ballast);
        navigate(HomeView.class);
        runPendingSignalsTasks();

        assertTrue(
                gauge(MeterNames.UI_STATE_NODES) >= nodesBefore
                        + ScalingSignalsView.GROWTH_STEP,
                "the kit's node gauge should reflect the added components: "
                        + nodesBefore + " -> "
                        + gauge(MeterNames.UI_STATE_NODES));
        // The headline claim of the use case: counting users tells you nothing
        // about capacity. One session and one tab throughout, while the state
        // held for that user jumped.
        assertEquals(uisBefore, gauge(MeterNames.UI_ACTIVE),
                "growing a tree must not change the UI count");
        assertEquals(sessionsBefore, gauge(MeterNames.SESSIONS_ACTIVE),
                "growing a tree must not change the session count");
    }

    @Test
    void theReadoutReportsTheKitsTotalRatherThanACountOfItsOwn() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        // UC3 no longer counts anything itself, so the grid value has to be
        // exactly what the kit's gauge says — including while that is stale.
        // The kit re-measures when an interaction ends, after this view's own
        // handler has run, so clicking cannot make the two disagree.
        test(findInView(Button.class).id("grow-state")).click();
        runPendingSignalsTasks();

        assertEquals(
                Long.toString(Math.round(gauge(MeterNames.UI_STATE_NODES))),
                capacityRow("UI state nodes (total)").value(),
                "the readout should mirror the kit's node gauge");
        assertEquals(
                Long.toString(Math.round(gauge(MeterNames.UI_STATE_VIEWS))),
                capacityRow("Retained view instances").value(),
                "the retained-view row should mirror the kit's gauge too");
    }

    @Test
    void measuringTheCostReportsTheValueToConfigure() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(Div.class).id("calibration-result").getText()
                        .startsWith("Not measured yet"),
                "the calibration readout should admit it has no measurement");
        assertEquals("not measured yet",
                capacityRow("Measured bytes per node").value(),
                "no measured cost should be claimed before measuring");

        test(findInView(Button.class).id("measure-cost")).click();
        runPendingSignalsTasks();

        String calibration = findInView(Div.class).id("calibration-result")
                .getText();
        assertTrue(calibration.contains("bytes per state node"),
                "the readout should report the value to configure: "
                        + calibration);
        // The kit is configured with a number; the probe's job is to say
        // whether
        // it still holds, so the readout must relate the two rather than just
        // print one.
        assertTrue(
                calibration.contains("ui-state-bytes-per-node")
                        || calibration.contains("configured"),
                "the readout should compare its measurement with the "
                        + "configured cost: " + calibration);
        assertTrue(
                capacityRow("Measured bytes per node").value().endsWith(" B"),
                "the measured cost should now be reported in bytes: "
                        + capacityRow("Measured bytes per node").value());
    }

    @Test
    void projectionRefusesToInventANumber() {
        // Every branch that cannot answer has to say why: this is the row a
        // capacity decision is actually read off, so a plausible-looking
        // fabricated number would be the worst possible failure.
        assertEquals("measure or configure bytes per node",
                ScalingSignalsView.projection(1024, 500, 1, 0),
                "with no cost per node there is nothing to project");
        assertEquals("heap gauges unavailable",
                ScalingSignalsView.projection(Double.NaN, 500, 1, 96),
                "without heap headroom there is nothing to divide");
        assertEquals("no UI state measured yet",
                ScalingSignalsView.projection(1024, Double.NaN, 1, 96),
                "without a node count there is no per-user cost");
        assertEquals("no UI state measured yet",
                ScalingSignalsView.projection(1024, 500, 0, 96),
                "with no UIs tracked the mean would divide by zero");

        // 1000 nodes over 2 UIs at 100 B is 50 000 B per UI; 500 000 B of
        // headroom is room for ten more.
        assertTrue(
                ScalingSignalsView.projection(500_000, 1000, 2, 100)
                        .startsWith("≈ 10 more UIs"),
                "the projection should divide headroom by per-UI cost, got: "
                        + ScalingSignalsView.projection(500_000, 1000, 2, 100));
    }

    @Test
    void heapPressureIsDerivedFromUsage() {
        // The badge's colour is a claim about capacity, so all four states are
        // pinned down here rather than left to whatever the test JVM happens to
        // be doing.
        assertEquals(Pressure.UNKNOWN,
                ScalingSignalsView.pressure(Double.NaN, 1024),
                "without heap gauges no claim can be made");
        assertEquals(Pressure.UNKNOWN, ScalingSignalsView.pressure(100, 0),
                "a zero maximum is not a usable ceiling");
        assertEquals(Pressure.COMFORTABLE,
                ScalingSignalsView.pressure(100, 1000),
                "10 % used leaves room for more users");
        assertEquals(Pressure.WATCH, ScalingSignalsView.pressure(700, 1000),
                "70 % used is the point to start planning");
        assertEquals(Pressure.TIGHT, ScalingSignalsView.pressure(900, 1000),
                "90 % used is too late to be planning");
    }

    @Test
    void pollingKeepsTheReadoutLiveAndStopsOnLeaving() {
        navigate(ScalingSignalsView.class);
        assertTrue(UI.getCurrent().getPollInterval() > 0,
                "the view should enable polling while attached");

        // Polling is what surfaces state the kit sampled after this view's own
        // handler ran, so a tick must rebuild the readout.
        UI ui = UI.getCurrent();
        ComponentUtil.fireEvent(ui, new PollEvent(ui, false));
        runPendingSignalsTasks();
        assertFalse(capacityRows().isEmpty(),
                "a poll tick should leave the readout populated");

        navigate(HomeView.class);
        assertEquals(-1, UI.getCurrent().getPollInterval(),
                "polling should be disabled again once the view is detached");
    }

    @Test
    void theExplanationColumnWrapsAndLongMeterNamesCanBreak() {
        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        // A Grid clips cell content by default, which cut the "why" column off
        // mid-sentence, and wrapping alone cannot help a meter name because it
        // is one unbroken token.
        Grid<Row> grid = findInView(Grid.class).id("capacity-grid");
        assertTrue(grid.getThemeNames().contains("wrap-cell-content"),
                "the capacity grid should wrap cell content, themes: "
                        + grid.getThemeNames());

        Span rendered = ScalingSignalsView
                .breakable(MeterNames.UI_STATE_SAMPLE_AGE_MAX);
        assertEquals(MeterNames.UI_STATE_SAMPLE_AGE_MAX, rendered.getText(),
                "the full meter name should be rendered");
        assertEquals("anywhere", rendered.getStyle().get("overflow-wrap"),
                "a long unbroken name must be allowed to break mid-token");

        List<Grid.Column<Row>> columns = grid.getColumns();
        assertEquals(1, columns.get(columns.size() - 1).getFlexGrow(),
                "the explanation column should absorb the remaining width");
        assertTrue(
                capacityRow("UI state nodes (total)").why()
                        .endsWith("tracks actual memory pressure."),
                "the readout should hold the whole explanation, not a summary");
    }

    private double gauge(String name) {
        Gauge gauge = registry.find(name).gauge();
        assertNotNull(gauge, name + " should be registered");
        return gauge.value();
    }

    @SuppressWarnings("unchecked")
    private List<Row> capacityRows() {
        Grid<Row> grid = findInView(Grid.class).id("capacity-grid");
        return grid.getListDataView().getItems().toList();
    }

    private Row capacityRow(String signal) {
        return capacityRows().stream()
                .filter(row -> signal.equals(row.signal())).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no readout row named: " + signal));
    }
}
