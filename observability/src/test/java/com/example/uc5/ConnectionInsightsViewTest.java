package com.example.uc5;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.home.HomeView;
import com.example.uc5.ConnectionInsightsView.ErrorInsight;
import com.example.uc5.ConnectionInsightsView.Stat;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.micrometer.ObservabilityKit;
import com.vaadin.observability.micrometer.ObservabilitySettings;
import com.vaadin.observability.micrometer.insights.CapturedClientError;
import com.vaadin.observability.micrometer.insights.ClientErrorCollector;
import com.vaadin.observability.micrometer.insights.RecentClientErrors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nothing in UC5 is measured by the application any more, so these tests assert
 * that the view reads the kit's two sources correctly: the connection meters
 * under their published names, and the {@code client-error} insights out of the
 * endpoint payload.
 * <p>
 * Both are driven the way the kit drives them — meters recorded as the
 * collector records them, errors captured through the kit's own
 * {@link ClientErrorCollector}, so the rows prove the view reads what the kit
 * really produces, frame parsing and detail gating included. What a browserless
 * test cannot reach is the browser half: the collector's subscription to
 * {@code window.Vaadin.connectionState}, its buffering across an outage and the
 * detail it gathers there are covered by the kit's own
 * {@code ClientProblemsIT}.
 */
@SpringBootTest
@ViewPackages(classes = { ConnectionInsightsView.class, HomeView.class })
// A fresh context per test, so the shared MeterRegistry and the kit's insight
// buffers start empty and a count can be asserted exactly rather than as a
// delta. Dirtied before each method rather than after: the first method would
// otherwise inherit a context an earlier @SpringBootTest class has already
// recorded into, and only the context this class is given can be relied on to
// be empty — the one it leaves behind is the next class's business.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ConnectionInsightsViewTest extends SpringBrowserlessTest {

    @Autowired
    MeterRegistry registry;

    @Autowired
    ObservabilitySettings settings;

    @Test
    void moduleKeepsTheKitsClientCollectionAndInsightsOn() {
        // Everything this view shows depends on these three. Client collection
        // feeds the connection meters and the error counter; insights retain
        // the errors themselves; and insights-details is what makes the
        // browser gather a message at all — with it off there is nothing to
        // withhold, so the message column would carry the kit's explanation
        // instead. If the module ever loses them, UC5 should fail here rather
        // than render a page of em dashes.
        assertTrue(settings.isClient(),
                "vaadin.observability.client should be on");
        assertTrue(settings.isInsights(),
                "vaadin.observability.insights should be on");
        assertTrue(settings.isInsightsDetails(),
                "this module enables insights-details for UC6, and UC5's "
                        + "message column depends on it");
    }

    @Test
    void rendersHeadingActionsAndBothReadouts() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(H1.class).all().stream()
                        .anyMatch(h -> h.getText().startsWith("UC5")),
                "UC5 heading should render");
        for (String id : new String[] { "simulate-loss",
                "simulate-reconnecting", "throw-error", "reject-promise",
                "refresh" }) {
            assertNotNull(findInView(Button.class).id(id),
                    "action should render: " + id);
        }
        assertTrue(errorRows().isEmpty(), "the insight table starts empty");
        assertTrue(
                findInView(Span.class).id("connection-status").getText()
                        .startsWith("Nothing recorded"),
                "the badge should say nothing has been recorded yet");
        assertTrue(
                findInView(Span.class).id("insights-status").getText()
                        .startsWith("No browser errors"),
                "and so should the insight status line");
    }

    @Test
    void readsTheKitsMetersUnderTheirPublishedNames() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        for (String meter : List.of(
                MeterNames.CLIENT_CONNECTION + " {connection-lost}",
                MeterNames.CLIENT_CONNECTION + " {reconnecting}",
                MeterNames.CLIENT_CONNECTION + " {connected}",
                MeterNames.CLIENT_CONNECTION_DOWNTIME + " {connection-lost}",
                MeterNames.CLIENT_CONNECTION_DOWNTIME + " {reconnecting}",
                MeterNames.CLIENT_ERRORS + " {uncaught}",
                MeterNames.CLIENT_ERRORS + " {promise}",
                MeterNames.RESYNC + " {resend}",
                MeterNames.RESYNC + " {resync}")) {
            assertNotNull(meterRow(meter), "readout row for " + meter);
        }
    }

    @Test
    void anUnrecordedMeterReadsAsADashRatherThanZero() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // "Nothing has happened" and "nothing is watching" are different
        // answers, and a browserless run has no collector at all.
        assertEquals("—",
                meterRow(MeterNames.CLIENT_CONNECTION + " {connection-lost}")
                        .value());
        assertEquals("—", meterRow(
                MeterNames.CLIENT_CONNECTION_DOWNTIME + " {reconnecting}")
                .value());
    }

    @Test
    void downtimeIsReadPerStateAndSummedIntoAWholeOutage() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // What the collector reports for one outage that Flow retried through
        // before giving up: time under each state, tagged separately.
        downtime(MeterNames.STATE_RECONNECTING).record(Duration.ofMillis(1500));
        downtime(MeterNames.STATE_CONNECTION_LOST)
                .record(Duration.ofMillis(3000));
        transitions(MeterNames.STATE_CONNECTION_LOST).increment();
        refresh();

        assertTrue(
                meterRow(MeterNames.CLIENT_CONNECTION_DOWNTIME
                        + " {connection-lost}").value()
                        .startsWith("1 period(s)"),
                "the given-up-on time should be read from its own tag");
        assertTrue(
                meterRow(MeterNames.CLIENT_CONNECTION_DOWNTIME
                        + " {reconnecting}").value().startsWith("1 period(s)"),
                "so should the retrying time");
        // The kit splits the timer because the two states mean different
        // things; adding them back is the application's job, and is the number
        // an SLO would use.
        assertEquals("%.1f s across both states".formatted(4.5),
                meterRow(MeterNames.CLIENT_CONNECTION_DOWNTIME
                        + " (sum of tags)").value(),
                "the whole outage is the sum, not either tag alone");
    }

    @Test
    void theBadgeSummarisesWhatTheKitRecorded() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        transitions(MeterNames.STATE_CONNECTION_LOST).increment();
        downtime(MeterNames.STATE_CONNECTION_LOST)
                .record(Duration.ofMillis(3000));
        refresh();

        Span status = findInView(Span.class).id("connection-status");
        assertTrue(status.getText().contains("1 loss(es)"),
                "the badge should count the losses: " + status.getText());
        assertTrue(status.getElement().getThemeList().contains("error"),
                "a recorded outage is not good news");
    }

    @Test
    void errorDetailComesFromTheKitsInsightsWithTheFrameAlreadyParsed() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // What the collector sends for one uncaught error, as the browser
        // wrote it: the frame is a whole stack line, not a location.
        capture("uncaught", "uc5", Map.of(ClientErrorCollector.DETAIL_ROUTE,
                "/uc5", ClientErrorCollector.DETAIL_MESSAGE,
                "Cannot read properties of undefined",
                ClientErrorCollector.DETAIL_SOURCE, "/VAADIN/build/chart.js:44",
                ClientErrorCollector.DETAIL_FRAME,
                "at renderChart (chart.js:44:13)"), 0);
        refresh();

        ErrorInsight insight = errorRows().getFirst();
        assertEquals("uc5", insight.route());
        assertEquals("uncaught", insight.kind());
        assertEquals("Cannot read properties of undefined", insight.message(),
                "the message the counter cannot carry");
        // The kit splits the stack line: the location is a location, and the
        // function name travels separately because a page can name a function
        // anything it likes.
        assertEquals("chart.js:44:13", insight.where(),
                "the view should show the location the kit parsed out");
        assertEquals("renderChart", insight.function());
        assertEquals(1, insight.occurrences());
    }

    @Test
    void repeatsOfTheSameErrorAreOneFindingWithACount() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // Two browsers hitting the same broken chart. The buffer is
        // application-wide and the grouping key is route, kind, source and
        // frame — no session or tab — so this is one finding, which is the
        // whole point of letting the kit retain them.
        capture("uncaught", "uc5", chartError(), 0);
        capture("uncaught", "uc5", chartError(), 0);
        // A different route is a different finding, even for the same script.
        capture("uncaught", "uc2", chartError(), 0);
        refresh();

        assertEquals(2, errorRows().size(),
                "same route groups, different route does not: " + errorRows());
        ErrorInsight grouped = errorRows().stream()
                .filter(row -> "uc5".equals(row.route())).findFirst()
                .orElseThrow();
        assertEquals(2, grouped.occurrences());
        assertTrue(
                findInView(Span.class).id("insights-status").getText()
                        .startsWith("2 grouped insight(s)"),
                "the status line should count the groups, not the reports");
    }

    @Test
    void aReportHeldThroughAnOutageSaysHowLongItWaited() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // The giveaway that an error could not be told to the server when it
        // was raised: the browser had lost it and held the report.
        capture("uncaught", "uc5", chartError(), 7400);
        capture("uncaught", "uc5", chartError(), 0);
        refresh();

        assertEquals(7400, errorRows().getFirst().maxBufferedMillis(),
                "the group reports the longest wait of its occurrences, so it "
                        + "speaks for at least one of them");
    }

    @Test
    void withheldDetailIsExplainedRatherThanLeftBlank() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // The default configuration, which this module overrides for UC6: with
        // insights-details off the browser never gathers a message, so there
        // is nothing to withhold. A blank cell would read like the error had no
        // message; the payload says which case it is, and the view shows it.
        buffer().add(new CapturedClientError(Instant.now(), "uc5", "uncaught",
                null, "/VAADIN/build/chart.js:44", "chart.js:44:13", null, 0,
                false, "hashed", 1));
        refresh();

        assertTrue(errorRows().getFirst().message().contains("not collected"),
                "the row should explain the absence: "
                        + errorRows().getFirst().message());
        assertEquals("chart.js:44:13", errorRows().getFirst().where(),
                "the location is published whatever the setting");
    }

    @Test
    void doesNotPoll() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // A poll is a UIDL request, so a polling view probes the connection on
        // every tick and ends an outage early — it would report shorter
        // downtime than a passive tab on the same network. The kit's README
        // says the same, and there is no ingest event to refresh from either,
        // so the refresh button is the only trigger.
        assertTrue(UI.getCurrent().getPollInterval() < 0,
                "polling would shorten the very outages this view reports");
    }

    @Test
    void aSecondSessionReadsTheSameApplicationWideBuffer() {
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        // A second browser. Re-initialising the Vaadin environment runs the
        // kit's service-init again, which builds a fresh insight buffer — a
        // harness artefact, since a production service is initialised once —
        // so what this checks is that the readout holds no per-view state and
        // reads the buffer the running service has. That two browsers'
        // identical errors become one finding is covered above, where the
        // buffer survives.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(ConnectionInsightsView.class);
        runPendingSignalsTasks();

        capture("promise", "uc5",
                Map.of(ClientErrorCollector.DETAIL_ROUTE, "/uc5",
                        ClientErrorCollector.DETAIL_MESSAGE,
                        "fetching /api/quotes failed"),
                0);
        refresh();

        assertEquals(1, errorRows().size(),
                "the second session's view should read the live buffer");
        assertEquals("promise", errorRows().getFirst().kind());
        // No stack frame and no script: a rejection has no filename at all,
        // and the page's own URL is not substituted for one.
        assertEquals("—", errorRows().getFirst().where(),
                "a location is published only when there is one");
    }

    // ---------- driving the kit ----------

    /**
     * Captures one browser error the way the kit's collector does, into the
     * buffer the running service is publishing from. Goes through
     * {@link ClientErrorCollector} rather than adding a record directly, so the
     * frame parsing and the detail gating are the kit's own.
     */
    private void capture(String kind, String route, Map<String, String> detail,
            long bufferedMs) {
        new ClientErrorCollector(buffer(), settings).capture(kind, route,
                detail, bufferedMs, UI.getCurrent());
    }

    private static Map<String, String> chartError() {
        return Map.of(ClientErrorCollector.DETAIL_ROUTE, "/uc5",
                ClientErrorCollector.DETAIL_MESSAGE,
                "Cannot read properties of undefined",
                ClientErrorCollector.DETAIL_SOURCE, "/VAADIN/build/chart.js:44",
                ClientErrorCollector.DETAIL_FRAME,
                "at renderChart (chart.js:44:13)");
    }

    /** The buffer the kit's service-init listener published for this app. */
    private static RecentClientErrors buffer() {
        RecentClientErrors buffer = ObservabilityKit.getRecentClientErrors();
        assertNotNull(buffer,
                "the kit should retain browser errors when client, insights "
                        + "and errors are all on");
        return buffer;
    }

    /** What the collector records for one connection-state transition. */
    private Counter transitions(String state) {
        return Counter.builder(MeterNames.CLIENT_CONNECTION)
                .tag(MeterNames.TAG_STATE, state).register(registry);
    }

    /** What it records for the time spent in one unreachable state. */
    private Timer downtime(String state) {
        return Timer.builder(MeterNames.CLIENT_CONNECTION_DOWNTIME)
                .tag(MeterNames.TAG_STATE, state).register(registry);
    }

    private void refresh() {
        test(findInView(Button.class).id("refresh")).click();
        runPendingSignalsTasks();
    }

    // ---------- reading the view back ----------

    @SuppressWarnings("unchecked")
    private List<ErrorInsight> errorRows() {
        Grid<ErrorInsight> grid = findInView(Grid.class).id("error-detail");
        return grid.getListDataView().getItems().toList();
    }

    @SuppressWarnings("unchecked")
    private Stat meterRow(String meter) {
        Grid<Stat> grid = findInView(Grid.class).id("problem-meters");
        return grid.getListDataView().getItems()
                .filter(stat -> meter.equals(stat.meter())).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no readout row for meter: " + meter));
    }
}
