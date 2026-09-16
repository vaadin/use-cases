package com.example.uc5;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.acme.AcmeCatalog;
import com.example.acme.AppWindow;
import com.example.home.HomeView;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.micrometer.ObservabilityKit;
import com.vaadin.observability.micrometer.ObservabilitySettings;
import com.vaadin.observability.micrometer.insights.CapturedClientError;
import com.vaadin.observability.micrometer.insights.ClientErrorCollector;
import com.vaadin.observability.micrometer.insights.RecentClientErrors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nothing in UC5 is measured by the application, so these tests assert two
 * things: that the Acme picking story works — the scene renders, the pick list
 * can be worked, and the investigation appears exactly when a picker's problem
 * is felt — and that the readout reads the kit's two sources correctly, the
 * connection meters under their published names and the {@code client-error}
 * insights out of the endpoint payload.
 * <p>
 * Both sources are driven the way the kit drives them: meters recorded as the
 * collector records them, errors captured through the kit's own
 * {@link ClientErrorCollector}, so the cards prove the view reads what the kit
 * really produces, frame parsing and detail gating included. What a browserless
 * test cannot reach is the browser half: the collector's subscription to
 * {@code window.Vaadin.connectionState}, its buffering across an outage and the
 * detail it gathers there are covered by the kit's own
 * {@code ClientProblemsIT}. The recovery callback the simulation script makes
 * is invoked here directly, since no script runs.
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

    private static final int METER = 0;
    private static final int TAGS = 1;
    private static final int REPORTS = 2;
    private static final int VALUE = 3;

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
        // withhold, so the card would carry the kit's explanation instead. If
        // the module ever loses them, UC5 should fail here rather than render
        // a page of em dashes.
        assertTrue(settings.isClient(),
                "vaadin.observability.client should be on");
        assertTrue(settings.isInsights(),
                "vaadin.observability.insights should be on");
        assertTrue(settings.isInsightsDetails(),
                "this module enables insights-details for UC6, and UC5's "
                        + "error detail depends on it");
    }

    @Test
    void opensWithThePickingScreenAndTheInvestigationHidden() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);

        assertEquals("UC5 — Why does the picking app freeze on the floor?",
                findInView(H1.class).first().getText());
        assertNotNull(findInView(AppWindow.class).first(),
                "the Acme picking scene is what makes the readout a story");
        assertEquals("AC-10731",
                findInView(TextField.class).id("order-number").getValue(),
                "the order on the tablet is part of the scene");
        assertEquals(ConnectionInsightsView.PICK_LIST.size(), pickRows().size(),
                "the whole pick list is on screen");
        assertTrue(pickRows().stream()
                .allMatch(row -> "Pending".equals(cell(row, 3))),
                "nothing is picked until the picker says so");
        assertNotNull(findInView(Div.class).id("simulation-rig"),
                "the outage is demo rigging, attached to the window rather "
                        + "than listed among the kit's own readouts");
        assertFalse(investigationOf(view).isVisible(),
                "the investigation appears only once a picker's problem has "
                        + "been felt");
    }

    @Test
    void theNumberedRouteIsAnAliasForTheStoryRoute() {
        navigate(ConnectionInsightsView.class);

        // The kit tags by the primary route template, so the story route is
        // primary ("picking" shows up in the telemetry) while the numbered URL
        // keeps working for stable demo links.
        assertEquals(ConnectionInsightsView.class,
                RouteConfiguration.forSessionScope().getRoute("uc5")
                        .orElseThrow(),
                "/uc5 must keep resolving to this view");
        assertEquals(ConnectionInsightsView.class,
                RouteConfiguration.forSessionScope()
                        .getRoute(ConnectionInsightsView.ROUTE).orElseThrow(),
                "/picking is the primary route the telemetry is tagged with");
    }

    @Test
    void everyPickListLineIsARealCatalogProduct() {
        // The scene has to read like the rest of the Acme application, not
        // like a fixture invented for this view.
        assertTrue(
                AcmeCatalog.products()
                        .containsAll(ConnectionInsightsView.PICK_LIST.stream()
                                .map(ConnectionInsightsView.Pick::item)
                                .toList()),
                "the pick list should come out of Acme's own catalog");
    }

    @Test
    void theSimulatedOutageIsLongEnoughToBeFelt() {
        // A sub-second freeze is not the complaint the pickers are making, and
        // an outage the reader does not notice explains nothing.
        navigate(ConnectionInsightsView.class);

        assertTrue(findInView(IntegerField.class).id("outage-length")
                .getValue() >= 1_000,
                "the simulated outage should last long enough to be felt");
    }

    @Test
    void confirmingAPickMarksTheLineWithoutRevealingTheInvestigation() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);

        confirmPick();

        assertEquals("Picked", cell(pickRows().get(0), 3));
        assertEquals("Pending", cell(pickRows().get(1), 3),
                "one click picks one line");
        assertFalse(investigationOf(view).isVisible(),
                "a pick that works is not a problem, so there is nothing to "
                        + "investigate yet");
    }

    @Test
    void aBrowserErrorRevealsTheInvestigation() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);

        click("show-stock");

        assertTrue(investigationOf(view).isVisible(),
                "a chart that never comes up is the picker's complaint, so it "
                        + "is the moment the investigation appears");
    }

    @Test
    void aRejectedPromiseRevealsTheInvestigationToo() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);

        click("sync-stock");

        assertTrue(investigationOf(view).isVisible());
    }

    @Test
    void losingTheConnectionRevealsTheInvestigation() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);

        click("simulate-loss");

        assertTrue(investigationOf(view).isVisible(),
                "the freeze is exactly what the pickers complain about");
    }

    @Test
    void walksTheInvestigationOneCollapsibleStepAtATime() {
        navigate(ConnectionInsightsView.class);
        click("show-stock");

        assertEquals(List.of("1 — Work the pick list"),
                findInView(H3.class).all().stream().map(H3::getText).toList(),
                "the story's own step is the only plain heading");
        List<Details> steps = findInView(Details.class).all();
        assertEquals(List.of("2 — The usual suspects see nothing",
                "3 — The kit's verdict", "4 — The raw meters, fleet-wide",
                "5 — What the numbers still cannot tell you"),
                steps.stream().map(Details::getSummaryText).toList(),
                "the readout is a guided investigation, not a meter dump");
        assertTrue(steps.get(0).isOpened(),
                "step 2 starts open: it is where the reader lands");
        assertFalse(steps.get(1).isOpened(),
                "step 3 waits until the reader has taken step 2");
        assertFalse(steps.get(2).isOpened());
        assertFalse(steps.get(3).isOpened());
        assertNotNull(findInView(Paragraph.class).id("server-side"),
                "step 2 shows what a server-side reader has to go on");

        openAllSteps();
        assertNotNull(findInView(Div.class).id("verdict"),
                "step 3 hosts the insights endpoint's findings");
        assertNotNull(findInView(Table.class).id("meter-table"),
                "step 4 keeps the raw meters as the drill-down");
    }

    @Test
    void theReadoutUsesNoGrid() {
        // The kit instruments every DataCommunicator, in-memory ones included.
        // A Grid showing the pick list or the meters would record data queries
        // on the very route whose client-side problems this view explains.
        navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        assertTrue(findInView(Grid.class).all().isEmpty(),
                "rendering this view must not issue data queries on its "
                        + "route");
    }

    @Test
    void theServerSideStepNamesTheErrorCounterAndTheResyncCounters() {
        navigate(ConnectionInsightsView.class);
        click("show-stock");

        String text = findInView(Paragraph.class).id("server-side").getElement()
                .getTextRecursively();
        assertTrue(text.contains("vaadin.errors"), text);
        assertTrue(text.contains("0 failure(s)"),
                "a browser error reaches no server-side counter: " + text);
        assertTrue(text.contains(MeterNames.RESYNC), text);
    }

    @Test
    void theVerdictExplainsItselfWhileEmpty() {
        navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        assertTrue(verdictText().contains("No browser errors reported yet"),
                "the empty verdict must say what would make a finding appear: "
                        + verdictText());
    }

    @Test
    void readsTheKitsMetersUnderTheirPublishedNames() {
        navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        assertEquals(List.of(
                MeterNames.CLIENT_CONNECTION + " state=connection-lost",
                MeterNames.CLIENT_CONNECTION + " state=reconnecting",
                MeterNames.CLIENT_CONNECTION + " state=connected",
                MeterNames.CLIENT_CONNECTION_DOWNTIME
                        + " state=connection-lost",
                MeterNames.CLIENT_CONNECTION_DOWNTIME + " state=reconnecting",
                MeterNames.CLIENT_CONNECTION_DOWNTIME + " state=* summed",
                MeterNames.CLIENT_ERRORS + " kind=uncaught",
                MeterNames.CLIENT_ERRORS + " kind=promise",
                MeterNames.RESYNC + " type=resend",
                MeterNames.RESYNC + " type=resync",
                MeterNames.CLIENT_DROPPED + " —",
                MeterNames.CLIENT_THROTTLED + " —"),
                rows().stream()
                        .map(row -> cell(row, METER) + " " + cell(row, TAGS))
                        .toList(),
                "the view exists to show these meters, by their tags");
    }

    @Test
    void anUnrecordedMeterReadsAsADashRatherThanZero() {
        navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        // "Nothing has happened" and "nothing is watching" are different
        // answers, and a browserless run has no collector at all — so the
        // meter does not exist rather than reading zero.
        assertEquals("—", reports(MeterNames.CLIENT_CONNECTION,
                "state=connection-lost"));
        assertEquals("—", value(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                "state=reconnecting"));
    }

    @Test
    void downtimeIsReadPerStateAndSummedIntoAWholeOutage() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        // What the collector reports for one outage that Flow retried through
        // before giving up: time under each state, tagged separately.
        downtime(MeterNames.STATE_RECONNECTING).record(Duration.ofMillis(1500));
        downtime(MeterNames.STATE_CONNECTION_LOST)
                .record(Duration.ofMillis(3000));
        transitions(MeterNames.STATE_CONNECTION_LOST).increment();
        view.connectionRestored();

        assertEquals("1", reports(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                "state=connection-lost"),
                "the given-up-on time should be read from its own tag");
        assertTrue(value(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                "state=reconnecting").contains("total, longest"),
                "so should the retrying time");
        // The kit splits the timer because the two states mean different
        // things; adding them back is the application's job, and is the number
        // an SLO would use.
        assertEquals("%.1f s across both states".formatted(4.5),
                value(MeterNames.CLIENT_CONNECTION_DOWNTIME,
                        "state=* summed"),
                "the whole outage is the sum, not either tag alone");
    }

    @Test
    void theSummaryCountsWhatTheKitRecorded() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("simulate-loss");
        openAllSteps();

        transitions(MeterNames.STATE_CONNECTION_LOST).increment();
        downtime(MeterNames.STATE_CONNECTION_LOST)
                .record(Duration.ofMillis(3000));
        view.connectionRestored();

        String text = findInView(Paragraph.class).id("connection-summary")
                .getElement().getTextRecursively();
        assertTrue(text.contains("1 loss(es)"),
                "the summary should count the losses: " + text);
    }

    @Test
    void errorDetailComesFromTheKitsInsightsWithTheFrameAlreadyParsed() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        // What the collector sends for one uncaught error, as the browser
        // wrote it: the frame is a whole stack line, not a location.
        capture("uncaught", ConnectionInsightsView.ROUTE,
                Map.of(ClientErrorCollector.DETAIL_ROUTE, "/picking",
                        ClientErrorCollector.DETAIL_MESSAGE,
                        "Cannot read properties of undefined",
                        ClientErrorCollector.DETAIL_SOURCE,
                        "/VAADIN/build/stock-chart.js:44",
                        ClientErrorCollector.DETAIL_FRAME,
                        "at renderChart (stock-chart.js:44:13)"),
                0);
        view.connectionRestored();

        String card = verdictText();
        assertEquals(1, cards(), "one report is one finding");
        assertTrue(card.contains("Cannot read properties of undefined"),
                "the message the counter cannot carry: " + card);
        // The kit splits the stack line: the location is a location, and the
        // function name travels separately because a page can name a function
        // anything it likes.
        assertTrue(card.contains("stock-chart.js:44:13"),
                "the location the kit parsed out: " + card);
        assertTrue(card.contains("in renderChart"), card);
        assertTrue(card.contains("route=picking"), card);
        assertTrue(card.contains("1×"), card);
    }

    @Test
    void repeatsOfTheSameErrorAreOneFindingWithACount() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        // Two tablets hitting the same broken chart. The buffer is
        // application-wide and the grouping key is route, kind, source and
        // frame — no session or tab — so this is one finding, which is the
        // whole point of letting the kit retain them.
        capture("uncaught", ConnectionInsightsView.ROUTE, chartError(), 0);
        capture("uncaught", ConnectionInsightsView.ROUTE, chartError(), 0);
        // A different screen is a different finding, even for the same script,
        // and this readout deliberately shows both.
        capture("uncaught", "orders", chartError(), 0);
        view.connectionRestored();

        assertEquals(2, cards(),
                "same route groups, different route does not: "
                        + verdictText());
        assertTrue(verdictText().contains("2×"),
                "the grouped finding should carry its occurrence count: "
                        + verdictText());
        assertTrue(verdictText().contains("route=orders"),
                "an error on another Acme screen is one nobody is watching "
                        + "either: " + verdictText());
    }

    @Test
    void aReportHeldThroughAnOutageSaysHowLongItWaited() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("simulate-loss");
        openAllSteps();

        // The giveaway that an error could not be told to the server when it
        // was raised: the tablet had lost it and held the report.
        capture("uncaught", ConnectionInsightsView.ROUTE, chartError(), 7400);
        capture("uncaught", ConnectionInsightsView.ROUTE, chartError(), 0);
        view.connectionRestored();

        assertTrue(verdictText().contains("held") && verdictText()
                .contains("offline"),
                "the group reports the longest wait of its occurrences, so it "
                        + "speaks for at least one of them: " + verdictText());
    }

    @Test
    void withheldDetailIsExplainedRatherThanLeftBlank() {
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        // The default configuration, which this module overrides for UC6: with
        // insights-details off the browser never gathers a message, so there
        // is nothing to withhold. A blank line would read like the error had no
        // message; the payload says which case it is, and the card shows it.
        buffer().add(new CapturedClientError(Instant.now(),
                ConnectionInsightsView.ROUTE, "uncaught", null,
                "/VAADIN/build/stock-chart.js:44", "stock-chart.js:44:13", null,
                0, false, "hashed", 1));
        view.connectionRestored();

        assertTrue(verdictText().contains("not collected"),
                "the card should explain the absence: " + verdictText());
        assertTrue(verdictText().contains("stock-chart.js:44:13"),
                "the location is published whatever the setting: "
                        + verdictText());
    }

    @Test
    void doesNotPoll() {
        navigate(ConnectionInsightsView.class);

        // A poll is a UIDL request, so a polling view probes the connection on
        // every tick and ends an outage early — it would report shorter
        // downtime than a passive tab on the same network. The kit's README
        // says the same, and there is no ingest event to refresh from either,
        // so the readout is recomputed from the interactions and from the
        // recovery callback instead.
        assertTrue(UI.getCurrent().getPollInterval() < 0,
                "polling would shorten the very outages this view reports");
    }

    @Test
    void aSecondSessionReadsTheSameApplicationWideBuffer() {
        navigate(ConnectionInsightsView.class);

        // A second tablet. Re-initialising the Vaadin environment runs the
        // kit's service-init again, which builds a fresh insight buffer — a
        // harness artefact, since a production service is initialised once —
        // so what this checks is that the readout holds no per-view state and
        // reads the buffer the running service has. That two tablets'
        // identical errors become one finding is covered above, where the
        // buffer survives.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        ConnectionInsightsView view = navigate(ConnectionInsightsView.class);
        click("show-stock");
        openAllSteps();

        capture("promise", ConnectionInsightsView.ROUTE,
                Map.of(ClientErrorCollector.DETAIL_ROUTE, "/picking",
                        ClientErrorCollector.DETAIL_MESSAGE,
                        "GET /api/warehouse/stock failed"),
                0);
        view.connectionRestored();

        assertEquals(1, cards(),
                "the second session's view should read the live buffer");
        assertTrue(verdictText().contains("promise"), verdictText());
        // No stack frame and no script: a rejection has no filename at all,
        // and the page's own URL is not substituted for one, so the kit's own
        // summary says so rather than naming a location.
        assertTrue(verdictText().contains("an unreported script"),
                "a location is published only when there is one: "
                        + verdictText());
    }

    // ---------- driving the view ----------

    private void click(String id) {
        test(findInView(Button.class).id(id)).click();
    }

    private void confirmPick() {
        click("confirm-pick");
    }

    /**
     * The investigation's steps are collapsible and only step 2 starts open;
     * tests that read the later steps' content open them all first, the way a
     * reader would.
     */
    private void openAllSteps() {
        findInView(Details.class).all().forEach(step -> step.setOpened(true));
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
        return Map.of(ClientErrorCollector.DETAIL_ROUTE, "/picking",
                ClientErrorCollector.DETAIL_MESSAGE,
                "Cannot read properties of undefined",
                ClientErrorCollector.DETAIL_SOURCE,
                "/VAADIN/build/stock-chart.js:44",
                ClientErrorCollector.DETAIL_FRAME,
                "at renderChart (stock-chart.js:44:13)");
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

    // ---------- reading the view back ----------

    private String verdictText() {
        return findInView(Div.class).id("verdict").getElement()
                .getTextRecursively();
    }

    private long cards() {
        return findInView(Div.class).id("verdict").getChildren().count();
    }

    private List<TableRow> pickRows() {
        return findInView(Table.class).id("pick-list").getBodyRows();
    }

    private List<TableRow> rows() {
        return findInView(Table.class).id("meter-table").getBodyRows();
    }

    private String reports(String meter, String tags) {
        return cellOf(meter, tags, REPORTS);
    }

    private String value(String meter, String tags) {
        return cellOf(meter, tags, VALUE);
    }

    private String cellOf(String meter, String tags, int index) {
        return rows().stream()
                .filter(row -> meter.equals(cell(row, METER))
                        && tags.equals(cell(row, TAGS)))
                .map(row -> cell(row, index)).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no row for " + meter + " " + tags));
    }

    private static String cell(TableRow row, int index) {
        // The meter, tag and value cells render their content as styled child
        // spans, so the cell's own text is empty and the text has to be read
        // from the whole subtree.
        return row.getDataCells().get(index).getElement().getTextRecursively();
    }

    /**
     * The investigation starts hidden, and hidden components may not be
     * reachable through the browserless queries, so it is located by walking
     * the component tree from the view instead.
     */
    private static Component investigationOf(Component root) {
        return findById(root).orElseThrow(() -> new AssertionError(
                "no component with id 'investigation'"));
    }

    private static Optional<Component> findById(Component root) {
        if (root.getId().filter("investigation"::equals).isPresent()) {
            return Optional.of(root);
        }
        return root.getChildren().map(ConnectionInsightsViewTest::findById)
                .flatMap(Optional::stream).findFirst();
    }
}
