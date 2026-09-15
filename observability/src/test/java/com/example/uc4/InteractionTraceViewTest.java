package com.example.uc4;

import java.util.List;
import java.util.Optional;

import com.example.acme.AppWindow;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note on coverage: unlike most of this module's use cases, the substance of
 * UC4 <em>is</em> reachable browserlessly. The kit's request and RPC spans are
 * not — a browserless click invokes the component listener directly rather than
 * through {@code ServerRpcHandler}, so no UIDL request is handled and neither
 * span exists (see the test-simulator note in {@code API-GAPS.md}) — but the
 * application's own dispatch, warehouse and carrier spans and the kit's
 * per-statement {@code vaadin.db.query} spans are opened by ordinary code on
 * the calling thread, which means the trail these tests assert on is a real
 * trail, only rooted one level lower than in a browser.
 */
@SpringBootTest
@ViewPackages(classes = { InteractionTraceView.class, HomeView.class })
class InteractionTraceViewTest extends SpringBrowserlessTest {

    private static final int SPAN = 0;
    private static final int DURATION = 2;
    private static final int ATTRIBUTES = 3;

    @Test
    void opensWithTheShippingDeskAndTheInvestigationHidden() {
        InteractionTraceView view = navigate(InteractionTraceView.class);

        assertEquals("UC4 — Where did this one dispatch spend its time?",
                findInView(H1.class).first().getText());
        assertNotNull(findInView(AppWindow.class).first(),
                "the Acme shipping desk scene is what makes the readout a "
                        + "story");
        assertEquals("Root & Branch Garden Centers",
                findInView(TextField.class).first().getValue(),
                "the consignee is prefilled so the first dispatch is one "
                        + "click away");
        assertEquals(3,
                findInView(Table.class).id("shipment-lines").getBodyRows()
                        .size(),
                "each line is a stock lookup, so the lines are what puts the "
                        + "datastore on the trail");
        assertNotNull(findInView(Button.class).withText("Dispatch shipment")
                .single());
        assertNotNull(findInView(Div.class).id("simulation-rig"),
                "the carrier's latency and its refusals are demo rigging, "
                        + "not a kit readout");
        assertFalse(investigationOf(view).isVisible(),
                "the investigation appears only once a dispatch has been "
                        + "felt");
    }

    @Test
    void theNumberedRouteIsAnAliasForTheStoryRoute() {
        navigate(InteractionTraceView.class);

        assertEquals(InteractionTraceView.class,
                RouteConfiguration.forSessionScope().getRoute("uc4")
                        .orElseThrow(),
                "/uc4 must keep resolving to this view");
        assertEquals(InteractionTraceView.class,
                RouteConfiguration.forSessionScope()
                        .getRoute(InteractionTraceView.ROUTE).orElseThrow(),
                "/shipping is the primary route the spans are attributed to");
    }

    @Test
    void theCarrierLatencyDefaultsUnderTheUxBudget() {
        // The opposite of UC1's, UC6's and UC8's rigs on purpose. Those exist
        // to cross the kit's 1 s budget so an insight is retained; this one
        // must stay under it, because UC4's whole point is the interaction
        // that no threshold catches and only a trail explains.
        navigate(InteractionTraceView.class);

        assertTrue(
                findInView(IntegerField.class).id("carrier-latency")
                        .getValue() < 1_000,
                "a dispatch that crossed the budget would be explained by "
                        + "UC1's and UC6's readouts instead");
    }

    @Test
    void theFirstDispatchRevealsTheInvestigation() {
        InteractionTraceView view = navigate(InteractionTraceView.class);

        dispatch();

        assertTrue(investigationOf(view).isVisible(),
                "the reveal happens before the work, so the readout lands in "
                        + "the same response as the dispatch it explains");
        assertTrue(
                findInView(Span.class).id("shipment-status").getText()
                        .contains("dispatched"),
                "a dispatch the carrier accepts is a dispatch");
    }

    @Test
    void walksTheInvestigationOneCollapsibleStepAtATime() {
        navigate(InteractionTraceView.class);
        dispatch();

        assertEquals(List.of("1 — Dispatch a shipment"),
                findInView(H3.class).all().stream().map(H3::getText).toList(),
                "the story's own step is the only plain heading");
        List<Details> steps = findInView(Details.class).all();
        assertEquals(
                List.of("2 — The trail of one dispatch",
                        "3 — Where the time went, and where it failed",
                        "4 — Where the trail begins, and where it ends"),
                steps.stream().map(Details::getSummaryText).toList());
        assertTrue(steps.get(0).isOpened(), "step 2 is where the reader lands");
        assertFalse(steps.get(1).isOpened());
        assertFalse(steps.get(2).isOpened());

        openAllSteps();
        assertNotNull(findInView(Div.class).id("trail-summary"));
        assertNotNull(findInView(Table.class).id("trail-table"));
        assertNotNull(findInView(Table.class).id("recent-trails"));
        assertNotNull(findInView(Div.class).id("trail-timing"));
        assertNotNull(findInView(Div.class).id("trail-limits"));
    }

    @Test
    void oneDispatchIsOneTrailAcrossTheApplicationAndTheDatastore() {
        navigate(InteractionTraceView.class);

        dispatch();

        List<String> spans = trailColumn(SPAN);
        assertTrue(spans.contains(InteractionTraceView.DISPATCH),
                "the business operation is the application's own span: "
                        + spans);
        assertTrue(spans.contains(WarehouseService.OBSERVATION),
                "the warehouse hop has to be named by the application, since "
                        + "the kit does not know it exists: " + spans);
        assertTrue(spans.contains(CarrierService.OBSERVATION),
                "the backend service at the far end of the trail: " + spans);
        assertTrue(spans.contains(InteractionTraceView.LABEL_PRINT),
                "a fast hop belongs on the trail too: " + spans);
        assertTrue(
                spans.stream().filter("vaadin.db.query"::equals)
                        .count() >= InteractionTraceView.LINES.size(),
                "one statement span per stock lookup, from the kit's "
                        + "database feature and no application code: " + spans);
        assertTrue(
                trailColumn(DURATION).stream()
                        .allMatch(value -> value.endsWith(" ms")),
                "every span reports how long it took");
    }

    @Test
    void theTrailIsIdentifiedByTheTraceIdItsLogLinesCarry() {
        navigate(InteractionTraceView.class);

        dispatch();

        String summary = findInView(Div.class).id("trail-summary").getElement()
                .getTextRecursively();
        assertTrue(summary.startsWith("Trace "), summary);
        assertTrue(summary.contains("spans over"), summary);
        assertTrue(summary.contains("traceId="),
                "the id the trail is read by is the id the log lines carry, "
                        + "which is what makes the two readable together: "
                        + summary);
    }

    @Test
    void theRankingNamesTheHopThatSpentTheTime() {
        navigate(InteractionTraceView.class);
        // Long enough to beat the local database and the label printer, short
        // enough not to pace the suite.
        findInView(IntegerField.class).id("carrier-latency").setValue(120);

        dispatchWithoutQuickening();
        openAllSteps();

        assertEquals(CarrierService.OBSERVATION,
                cell(findInView(Table.class).id("trail-ranking").getBodyRows()
                        .get(0), SPAN),
                "the ranking is by own time, so the carrier's 120 ms has to "
                        + "come first — ahead of the dispatch span that spent "
                        + "them waiting for it");
        String timing = findInView(Div.class).id("trail-timing").getElement()
                .getTextRecursively();
        assertTrue(timing.contains("on its own"), timing);
        assertTrue(timing.contains("vaadin.errors"),
                "the counter the meters offer instead belongs next to it: "
                        + timing);
    }

    @Test
    void aRefusedBookingFailsTheCarrierSpanAndNotTheRequest() {
        navigate(InteractionTraceView.class);
        findInView(Checkbox.class).id("carrier-refuses").setValue(true);

        // No exception escapes: the desk handles the refusal, which is why no
        // meter and no insight ever sees it.
        dispatch();

        assertTrue(
                findInView(Span.class).id("shipment-status").getText()
                        .contains("carrier refused"),
                "the clerk is told the booking failed");
        assertTrue($(Notification.class).all().stream()
                .anyMatch(Notification::isOpened));
        TableRow carrier = trailRow(CarrierService.OBSERVATION);
        assertTrue(carrier.hasClassName("trail-failed"),
                "the failing hop is marked on the trail");
        assertTrue(cell(carrier, SPAN).contains("no capacity on this lane"),
                "and it carries the failure the caller swallowed: "
                        + cell(carrier, SPAN));
        assertFalse(
                trailColumn(SPAN).stream()
                        .anyMatch(span -> span
                                .startsWith(InteractionTraceView.LABEL_PRINT)),
                "the label printer is never reached, and a trail shows what "
                        + "ran rather than what was meant to");
    }

    @Test
    void theFailureIsAttributedToTheInnermostHopThatCarriesIt() {
        navigate(InteractionTraceView.class);
        findInView(Checkbox.class).id("carrier-refuses").setValue(true);

        dispatch();
        openAllSteps();

        // The refusal is let out of the carrier hop, so it marks the business
        // span above it too. The outermost red span is the least informative
        // of them; the question is which hop introduced the failure.
        assertTrue(
                trailRow(InteractionTraceView.DISPATCH)
                        .hasClassName("trail-failed"),
                "a failure let out of a hop marks its caller as well");
        assertTrue(
                findInView(Div.class).id("trail-timing").getElement()
                        .getTextRecursively()
                        .contains("entered the trail at "
                                + CarrierService.OBSERVATION),
                "but the readout must name the innermost one: "
                        + findInView(Div.class).id("trail-timing").getElement()
                                .getTextRecursively());
    }

    @Test
    void pollsSoTheRequestSpanCanArriveAfterItsOwnResponse() {
        navigate(InteractionTraceView.class);

        // The kit stops the request observation in requestEnd, after the
        // response has been written, so the root of a dispatch's trail cannot
        // be in the response that dispatch produced.
        assertEquals(2_000, UI.getCurrent().getPollInterval());

        navigate(HomeView.class);
        assertEquals(-1, UI.getCurrent().getPollInterval(),
                "the poll belongs to this view, not to the UI it borrowed");
    }

    @Test
    void theTrailsAreApplicationScopedAcrossSessions() {
        // The trails come from an application-scoped span reporter, not from
        // per-session state: a second session reads the ones the first one
        // left behind, which is what a colleague looking over your shoulder
        // needs. Kept under MAX_TRAILS so the count can be compared at all.
        navigate(InteractionTraceView.class);
        dispatch();
        String firstSessionTrace = cell(recentTrails().get(0), 0);

        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(InteractionTraceView.class);
        dispatch();

        List<String> traces = recentTrails().stream().map(row -> cell(row, 0))
                .toList();
        assertTrue(traces.contains(firstSessionTrace),
                "the second session's dispatch joins the first session's "
                        + "rather than replacing it: " + traces);
        assertNotEquals(firstSessionTrace, traces.get(0),
                "newest first, and the newest is this session's");
        assertTrue(findInView(Div.class).id("trail-summary").getElement()
                .getTextRecursively().contains(traces.get(0).replace("…", "")),
                "and the trail this view follows is its own dispatch: "
                        + traces.get(0));
    }

    @Test
    void theTrailIsNotAGrid() {
        // The kit instruments every DataCommunicator, in-memory ones
        // included, and with tracing on a Grid rendering the trail would open
        // data-query spans on the very route whose trail it draws.
        navigate(InteractionTraceView.class);
        dispatch();
        openAllSteps();

        assertTrue(findInView(Grid.class).all().isEmpty(),
                "rendering this view must not add spans to what it explains");
    }

    @Test
    void theCarrierCanBeChosen() {
        navigate(InteractionTraceView.class);
        quickenTheCarrier();

        test(findInView(Select.class).id("carrier"), String.class)
                .selectItem(InteractionTraceView.CARRIER_RAIL);
        clickDispatch();

        assertTrue(findInView(Span.class).id("shipment-status").getText()
                .contains(InteractionTraceView.CARRIER_RAIL));
        assertTrue(cell(trailRow(InteractionTraceView.DISPATCH), ATTRIBUTES)
                .contains("acme.carrier=" + InteractionTraceView.CARRIER_RAIL),
                "the carrier is low cardinality, so it rides along as a span "
                        + "attribute and could be a meter tag too");
    }

    /** A dispatch with the carrier answering instantly, as a test wants. */
    private void dispatch() {
        quickenTheCarrier();
        clickDispatch();
    }

    private void dispatchWithoutQuickening() {
        clickDispatch();
    }

    private void clickDispatch() {
        test(findInView(Button.class).withText("Dispatch shipment").single())
                .click();
    }

    private void quickenTheCarrier() {
        findInView(IntegerField.class).id("carrier-latency").setValue(0);
    }

    private void openAllSteps() {
        findInView(Details.class).all().forEach(step -> step.setOpened(true));
    }

    private List<TableRow> recentTrails() {
        return findInView(Table.class).id("recent-trails").getBodyRows();
    }

    private List<TableRow> trailRows() {
        return findInView(Table.class).id("trail-table").getBodyRows();
    }

    private List<String> trailColumn(int index) {
        return trailRows().stream().map(row -> cell(row, index)).toList();
    }

    /** The trail's row for one span name. */
    private TableRow trailRow(String spanName) {
        return trailRows().stream()
                .filter(row -> cell(row, SPAN).startsWith(spanName)).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no row for " + spanName + " in " + trailColumn(SPAN)));
    }

    private static String cell(TableRow row, int index) {
        // The cells render their content as styled child components, so the
        // cell's own text is empty and has to be read from the subtree.
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
        return root.getChildren().map(InteractionTraceViewTest::findById)
                .flatMap(Optional::stream).findFirst();
    }
}
