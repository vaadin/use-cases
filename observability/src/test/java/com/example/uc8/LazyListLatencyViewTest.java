package com.example.uc8;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.example.acme.AppWindow;
import com.example.home.HomeView;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { LazyListLatencyView.class, HomeView.class })
class LazyListLatencyViewTest extends SpringBrowserlessTest {

    private static final int METER = 0;
    private static final int TAGS = 1;
    private static final int QUERIES = 2;
    private static final int VALUE = 3;

    @Autowired
    MeterRegistry registry;

    @Test
    void opensWithTheOrderDeskAndTheInvestigationHidden() {
        LazyListLatencyView view = navigate(LazyListLatencyView.class);

        assertEquals("UC8 — Why is the product search slow?",
                findInView(H1.class).first().getText());
        assertNotNull(findInView(AppWindow.class).first(),
                "the Acme order desk scene is what makes the readout a story");
        assertNotNull(findInView(ComboBox.class).first(),
                "the lazy product search is the subject of the use case");
        assertNotNull(findInView(TextField.class).first(),
                "the prefilled customer is part of the order desk scene");
        assertNotNull(findInView(Table.class).id("order-lines"));
        assertNotNull(findInView(Div.class).id("simulation-rig"),
                "the latency knob is demo rigging, attached to the window "
                        + "rather than listed among the kit's own readouts");
        assertFalse(investigationOf(view).isVisible(),
                "the investigation appears only once the reader has felt "
                        + "the wait it explains");
    }

    @Test
    void theNumberedRouteIsAnAliasForTheStoryRoute() {
        navigate(LazyListLatencyView.class);

        // The kit tags meters by the primary route template, so the story
        // route is primary ("orders" shows up in the telemetry) while the
        // numbered URL keeps working for stable demo links.
        assertEquals(LazyListLatencyView.class,
                RouteConfiguration.forSessionScope().getRoute("uc8")
                        .orElseThrow(),
                "/uc8 must keep resolving to this view");
        assertEquals(LazyListLatencyView.class,
                RouteConfiguration.forSessionScope()
                        .getRoute(LazyListLatencyView.ROUTE).orElseThrow(),
                "/orders is the primary route the meters are tagged with");
    }

    @Test
    void theFirstCatalogQueryRevealsTheInvestigation() {
        LazyListLatencyView view = navigate(LazyListLatencyView.class);

        searchTheCatalog();

        assertTrue(investigationOf(view).isVisible(),
                "querying the catalog is the moment the wait is felt, so it "
                        + "is the moment the investigation appears");
    }

    @Test
    void walksTheInvestigationOneCollapsibleStepAtATime() {
        navigate(LazyListLatencyView.class);
        searchTheCatalog();

        assertEquals(List.of("1 — Take an order"),
                findInView(H3.class).all().stream().map(H3::getText).toList(),
                "the story's own step is the only plain heading");
        List<Details> steps = findInView(Details.class).all();
        assertEquals(List.of("2 — The usual suspects look innocent",
                "3 — The kit's verdict", "4 — The raw meters, fleet-wide"),
                steps.stream().map(Details::getSummaryText).toList(),
                "the readout is a guided investigation, not a meter dump");
        assertTrue(steps.get(0).isOpened(),
                "step 2 starts open: it is where the reader lands");
        assertFalse(steps.get(1).isOpened(),
                "step 3 waits until the reader has taken step 2");
        assertFalse(steps.get(2).isOpened(),
                "step 4 waits until the reader has taken step 3");
        assertNotNull(findInView(Paragraph.class).id("innocent-timers"),
                "step 2 shows why the interaction timers cannot explain "
                        + "a slow search");

        openAllSteps();
        assertNotNull(findInView(Div.class).id("verdict"),
                "step 3 hosts the insights endpoint's findings");
        assertNotNull(findInView(Table.class).id("meter-table"),
                "step 4 keeps the raw meters as the drill-down");
    }

    @Test
    void theVerdictExplainsItselfWhileEmpty() {
        // A browserless click bypasses the kit's capture pipeline (see UC6's
        // note), so the verdict can only be asserted in its empty state here;
        // the populated card is covered by using the running application.
        navigate(LazyListLatencyView.class);
        searchTheCatalog();
        openAllSteps();

        Div verdict = findInView(Div.class).id("verdict");
        assertTrue(verdict.getChildren().findFirst().orElseThrow().getElement()
                .getText().contains("UX budget"),
                "the empty verdict must say what would make a finding appear");
    }

    @Test
    void addToOrderAppendsALineAndClearsTheSearch() {
        navigate(LazyListLatencyView.class);

        ComboBox<?> product = findInView(ComboBox.class).first();
        IntegerField quantity = findInView(IntegerField.class)
                .id("order-quantity");
        quickenTheCatalog();
        test(product, String.class).selectItem("Brass hex bolt M8 × 40");
        quantity.setValue(3);

        test(findInView(Button.class).withText("Add to order").single())
                .click();

        Table orderLines = findInView(Table.class).id("order-lines");
        List<TableRow> lines = orderLines.getBodyRows();
        assertEquals(1, lines.size());
        assertEquals("Brass hex bolt M8 × 40",
                lines.get(0).getDataCells().get(0).getText());
        assertEquals("3", lines.get(0).getDataCells().get(1).getText());
        assertNull(product.getValue(),
                "the search clears so the clerk can type the next product");
    }

    @Test
    void neitherTableIsAGrid() {
        // The kit instruments every DataCommunicator, in-memory ones included,
        // and tags the row summaries by route. A Grid showing the order lines
        // or the meters would record a count and a fetch on route orders at
        // every refresh, perturbing the very numbers the readout displays.
        navigate(LazyListLatencyView.class);

        assertTrue(findInView(Grid.class).all().isEmpty(),
                "rendering this view must not issue data queries on its "
                        + "route");
    }

    @Test
    void theMeterTableNamesTheFourDataQueryMetersByTheirTags() {
        navigate(LazyListLatencyView.class);
        searchTheCatalog();
        openAllSteps();

        assertEquals(List.of("vaadin.data.count.duration",
                "vaadin.data.count.duration", "vaadin.data.fetch.duration",
                "vaadin.data.fetch.duration", "vaadin.data.fetch.requested",
                "vaadin.data.fetch.rows"), column(METER),
                "the view exists to show these four meters");
        assertEquals(
                List.of("filtered=true", "filtered=false", "filtered=true",
                        "filtered=false", "route=orders", "route=orders"),
                column(TAGS),
                "the timers have no route tag and are split by filtered "
                        + "instead; the summaries are scoped to this route");
    }

    @Test
    void theSimulatedLatencyDefaultsAboveTheInsightsBudget() {
        // The kit reports a slow data query only above its 1 s UX budget. A
        // default below it would make the verdict show nothing on the first
        // try, which is the exact dead end this view exists to avoid.
        navigate(LazyListLatencyView.class);

        IntegerField delay = findInView(IntegerField.class)
                .id("backend-delay");
        assertTrue(delay.getValue() > 1_000,
                "the first search must already exceed the insights budget");
    }

    @Test
    void anUnmeasuredMeterReadsAsADashRatherThanZero() {
        // The registry is shared across the tests in this context, so this
        // asserts the formatting rule rather than that nothing has run yet.
        navigate(LazyListLatencyView.class);
        searchTheCatalog();
        openAllSteps();

        List<TableRow> rows = rows();
        assertFalse(rows.isEmpty());
        assertTrue(
                rows.stream().filter(row -> "0".equals(cell(row, QUERIES)))
                        .allMatch(row -> "—".equals(cell(row, VALUE))),
                "a meter with no recordings shows a dash, not 0 ms");
    }

    @Test
    void theReadoutReadsOnlyTheFilteredTimersAndThisRoutesSummaries() {
        navigate(LazyListLatencyView.class);
        searchTheCatalog();
        openAllSteps();

        // What the kit would record for the product search matching typed
        // text.
        timer("vaadin.data.count.duration", "true").record(120,
                TimeUnit.MILLISECONDS);
        timer("vaadin.data.fetch.duration", "true").record(80,
                TimeUnit.MILLISECONDS);
        summary("vaadin.data.fetch.requested", "orders").record(50);
        summary("vaadin.data.fetch.rows", "orders").record(50);
        // What an in-memory grid on some other view would record: no filter,
        // another route. None of it may leak into the product search's rows.
        timer("vaadin.data.count.duration", "false").record(9_000,
                TimeUnit.MILLISECONDS);
        timer("vaadin.data.fetch.duration", "false").record(9_000,
                TimeUnit.MILLISECONDS);
        summary("vaadin.data.fetch.requested", "uc2").record(999);
        summary("vaadin.data.fetch.rows", "uc2").record(999);

        // There is no refresh button: touching the catalog recomputes the
        // readout, exactly as it does for a person driving the demo.
        searchTheCatalog();

        assertEquals("mean 120 ms, max 120 ms",
                value("vaadin.data.count.duration", "filtered=true"));
        assertEquals("mean 80 ms, max 80 ms",
                value("vaadin.data.fetch.duration", "filtered=true"));
        assertEquals("50 items over 1 fetches",
                value("vaadin.data.fetch.requested", "route=orders"));
        assertEquals("50 items over 1 fetches",
                value("vaadin.data.fetch.rows", "route=orders"));
        // The unfiltered rows are still shown, so that the app-wide cost is
        // not hidden, but kept apart from the product search's own. Other
        // views' in-memory grids in this shared Spring context record here
        // too, which is exactly the leak the row exists to show, so only the
        // distinctive maximum is asserted rather than the mean.
        assertTrue(
                value("vaadin.data.count.duration", "filtered=false")
                        .endsWith("max 9000 ms"),
                "the unfiltered count row must include the foreign 9 s");
        assertTrue(
                value("vaadin.data.fetch.duration", "filtered=false")
                        .endsWith("max 9000 ms"),
                "the unfiltered fetch row must include the foreign 9 s");
    }

    /**
     * Drives the reveal the way a person does: by searching the catalog. The
     * simulated latency is zeroed first so the suite is not paced by
     * {@code Thread.sleep}.
     */
    private void searchTheCatalog() {
        quickenTheCatalog();
        test(findInView(ComboBox.class).first(), String.class)
                .selectItem("Brass hex bolt M8 × 40");
    }

    /**
     * The investigation's steps are collapsible and only step 2 starts open;
     * tests that read the later steps' content open them all first, the way a
     * reader would.
     */
    private void openAllSteps() {
        findInView(Details.class).all()
                .forEach(step -> step.setOpened(true));
    }

    private void quickenTheCatalog() {
        findInView(IntegerField.class).id("backend-delay").setValue(0);
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
        return root.getChildren().map(LazyListLatencyViewTest::findById)
                .flatMap(Optional::stream).findFirst();
    }

    private Timer timer(String name, String filtered) {
        return Timer.builder(name).tag("outcome", "success")
                .tag("filtered", filtered).register(registry);
    }

    private DistributionSummary summary(String name, String route) {
        return DistributionSummary.builder(name).tag("route", route)
                .register(registry);
    }

    private List<TableRow> rows() {
        return findInView(Table.class).id("meter-table").getBodyRows();
    }

    private List<String> column(int index) {
        return rows().stream().map(row -> cell(row, index)).toList();
    }

    private String value(String meter, String tags) {
        return rows().stream()
                .filter(row -> meter.equals(cell(row, METER))
                        && tags.equals(cell(row, TAGS)))
                .map(row -> cell(row, VALUE)).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no row for " + meter + " " + tags));
    }

    private static String cell(TableRow row, int index) {
        // The meter, tag and value cells render their content as styled child
        // spans, so the cell's own text is empty and the text has to be read
        // from the whole subtree.
        return row.getDataCells().get(index).getElement()
                .getTextRecursively();
    }
}
