package com.example.uc1;

import java.util.List;
import java.util.Optional;

import com.example.acme.AppWindow;
import com.example.home.HomeView;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.RouteConfiguration;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note on coverage: the kit captures an interaction from Flow's RPC invocation
 * listener, which only fires while handling a real UIDL request, so the
 * populated verdict (step 3) is covered by using the running application. The
 * application's own action timer records in the click listener itself, so
 * step 4 is asserted for real here.
 */
@SpringBootTest
@ViewPackages(classes = { InteractionLatencyView.class, HomeView.class })
class InteractionLatencyViewTest extends SpringBrowserlessTest {

    @Autowired
    MeterRegistry registry;

    @Test
    void opensWithTheInvoicingDeskAndTheInvestigationHidden() {
        InteractionLatencyView view = navigate(InteractionLatencyView.class);

        assertTrue(findInView(H1.class).first().getText().startsWith("UC1"));
        assertNotNull(findInView(AppWindow.class).first(),
                "the Acme invoicing desk is what makes the readout a story");
        for (String label : List.of("Save draft", "Apply discounts",
                "Issue invoice")) {
            assertEquals(1,
                    findInView(Button.class).withText(label).all().size(),
                    "action should render: " + label);
        }
        assertNotNull(findInView(IntegerField.class).id("tax-delay"),
                "the tax service latency is demo rigging, not a kit readout");
        assertFalse(investigationOf(view).isVisible(),
                "the investigation appears only once an action has been felt");
    }

    @Test
    void theNumberedRouteIsAnAliasForTheStoryRoute() {
        navigate(InteractionLatencyView.class);

        assertEquals(InteractionLatencyView.class,
                RouteConfiguration.forSessionScope().getRoute("uc1")
                        .orElseThrow());
        assertEquals(InteractionLatencyView.class,
                RouteConfiguration.forSessionScope()
                        .getRoute(InteractionLatencyView.ROUTE).orElseThrow(),
                "/invoices is the primary route the insights carry");
    }

    @Test
    void theTaxServiceDefaultsAboveTheInsightsBudget() {
        navigate(InteractionLatencyView.class);

        assertTrue(findInView(IntegerField.class).id("tax-delay")
                .getValue() > 1_000,
                "issuing an invoice must exceed the 1 s UX budget on the first "
                        + "try, or step 3 has nothing to show");
    }

    @Test
    void theFirstActionRevealsTheInvestigationAndUpdatesTheDesk() {
        InteractionLatencyView view = navigate(InteractionLatencyView.class);

        click("Save draft");

        assertTrue(investigationOf(view).isVisible());
        assertEquals("Draft saved",
                findInView(Span.class).id("invoice-status").getText());
    }

    @Test
    void walksTheInvestigationOneCollapsibleStepAtATime() {
        navigate(InteractionLatencyView.class);
        click("Save draft");

        assertEquals(List.of("1 — Work an invoice"),
                findInView(H3.class).all().stream().map(H3::getText).toList());
        List<Details> steps = findInView(Details.class).all();
        assertEquals(List.of("2 — What the framework times",
                "3 — The kit's verdict", "4 — Per action, the app's own timer"),
                steps.stream().map(Details::getSummaryText).toList());
        assertTrue(steps.get(0).isOpened());
        assertFalse(steps.get(1).isOpened());
        assertFalse(steps.get(2).isOpened());
        assertTrue(findInView(Grid.class).all().isEmpty(),
                "the readout uses no Grid, so it records no data queries on "
                        + "the route whose latency it explains");
    }

    @Test
    void theFrameworkTimersAreListedEvenBeforeAnySample() {
        navigate(InteractionLatencyView.class);
        click("Save draft");

        List<String> meters = findInView(Table.class).id("framework-timers")
                .getBodyRows().stream().map(row -> cell(row, 0)).toList();
        assertEquals(List.of("vaadin.request.duration", "vaadin.rpc.duration",
                "vaadin.client.request.duration",
                "vaadin.client.render.duration",
                "vaadin.client.navigation.duration",
                "vaadin.client.web_vitals.lcp",
                "vaadin.client.web_vitals.fcp"), meters,
                "the framework and browser timers are always listed, with a "
                        + "dash until they have samples");
    }

    @Test
    void theBrowserRowsReadThisRouteOnly() {
        navigate(InteractionLatencyView.class);
        click("Save draft");

        List<TableRow> rows = findInView(Table.class).id("framework-timers")
                .getBodyRows();
        assertEquals("route=invoices", tagsOf(rows,
                "vaadin.client.request.duration"),
                "the browser's round trip is read for this route, the tag "
                        + "the kit puts on it");
        assertEquals("route=invoices",
                tagsOf(rows, "vaadin.client.render.duration"));
        assertEquals("vaadin.request.type=uidl",
                tagsOf(rows, "vaadin.request.duration"),
                "the server row is scoped to the same requests the browser "
                        + "times, so the two describe one population");
    }

    @Test
    void theRenderRowSaysWhyItIsEmptyWhenRoundTripsAreThere() {
        // The browserless harness has no browser collector, so the sample is
        // recorded the way the kit's binder would record it.
        registry.timer("vaadin.client.request.duration", "route", "invoices")
                .record(Duration.ofMillis(180));
        navigate(InteractionLatencyView.class);
        click("Save draft");

        List<TableRow> rows = findInView(Table.class).id("framework-timers")
                .getBodyRows();
        assertTrue(readsOf(rows, "vaadin.client.render.duration")
                .contains("vaadin.requestTiming=true"),
                "round trips without render figures can only mean Flow's "
                        + "request timing is off, and the row should say so");
    }

    @Test
    void theNetworkShareIsWorkedOutFromBothEnds() {
        registry.timer("vaadin.request.duration", "vaadin.request.type",
                "uidl", "http.method", "POST", "outcome", "success",
                "vaadin.interaction", "rpc", "error", "none")
                .record(Duration.ofMillis(120));
        registry.timer("vaadin.client.request.duration", "route", "invoices")
                .record(Duration.ofMillis(180));
        navigate(InteractionLatencyView.class);
        click("Save draft");

        String share = findInView(Div.class).id("wire-share").getElement()
                .getTextRecursively();
        assertTrue(share.contains("60.0 ms"),
                "the network's share is the browser's round trip less the "
                        + "server's handling of the same requests: " + share);
    }

    @Test
    void eachActionGetsItsOwnRowInTheAppsTimer() {
        // The timer lives in the registry every test in this class shares,
        // so the counts are read as the difference this test makes, not as
        // absolute values that depend on which tests ran before it.
        long savesBefore = clicks(InteractionLatencyView.ACTION_SAVE);
        long issuesBefore = clicks(InteractionLatencyView.ACTION_ISSUE);
        navigate(InteractionLatencyView.class);
        findInView(IntegerField.class).id("tax-delay").setValue(0);

        click("Save draft");
        click("Issue invoice");
        click("Issue invoice");
        openAllSteps();

        List<TableRow> rows = findInView(Table.class).id("action-timers")
                .getBodyRows();
        assertEquals(String.valueOf(savesBefore + 1), countOf(rows,
                "action=" + InteractionLatencyView.ACTION_SAVE));
        assertEquals(String.valueOf(issuesBefore + 2), countOf(rows,
                "action=" + InteractionLatencyView.ACTION_ISSUE),
                "the business action timer counts the clicks per action — "
                        + "the granularity the kit's meters do not carry");
        assertEquals("Invoice INV-24312 issued",
                findInView(Span.class).id("invoice-status").getText());
    }

    @Test
    void theVerdictExplainsItselfWhileEmpty() {
        navigate(InteractionLatencyView.class);
        click("Save draft");
        openAllSteps();

        assertTrue(findInView(Div.class).id("verdict").getElement()
                .getTextRecursively().contains("UX budget"),
                "the empty verdict must say what would make a finding appear");
    }

    @Test
    void pollingStopsAfterLeavingTheView() {
        navigate(InteractionLatencyView.class);
        assertTrue(UI.getCurrent().getPollInterval() > 0,
                "the view enables polling while attached");

        navigate(HomeView.class);
        assertEquals(-1, UI.getCurrent().getPollInterval(),
                "polling is disabled again once the view is detached");
    }

    @Test
    void theActionTimerIsSharedAcrossSessions() {
        // Session 1 issues an invoice; the app's timer records into the
        // application-scoped registry.
        navigate(InteractionLatencyView.class);
        findInView(IntegerField.class).id("tax-delay").setValue(0);
        click("Apply discounts");

        // Session 2: a fresh Vaadin environment, same Spring context. The
        // action recorded by session 1 must be visible.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(InteractionLatencyView.class);
        click("Save draft");
        openAllSteps();

        assertTrue(findInView(Table.class).id("action-timers").getBodyRows()
                .stream().anyMatch(row -> cell(row, 1).equals(
                        "action=" + InteractionLatencyView.ACTION_DISCOUNTS)),
                "an action recorded in the first session should be visible "
                        + "in the second");
    }

    private void click(String label) {
        test(findInView(Button.class).withText(label).single()).click();
    }

    private void openAllSteps() {
        findInView(Details.class).all()
                .forEach(step -> step.setOpened(true));
    }

    private long clicks(String action) {
        return registry.find(InteractionLatencyView.ACTION_TIMER)
                .tag(InteractionLatencyView.TAG_ACTION, action).timers()
                .stream().mapToLong(t -> t.count()).sum();
    }

    private static String countOf(List<TableRow> rows, String tags) {
        return rows.stream().filter(row -> cell(row, 1).equals(tags))
                .map(row -> cell(row, 2)).findFirst()
                .orElseThrow(() -> new AssertionError("no row " + tags));
    }

    private static String tagsOf(List<TableRow> rows, String meter) {
        return rows.stream().filter(row -> cell(row, 0).equals(meter))
                .map(row -> cell(row, 1)).findFirst()
                .orElseThrow(() -> new AssertionError("no row " + meter));
    }

    private static String readsOf(List<TableRow> rows, String meter) {
        return rows.stream().filter(row -> cell(row, 0).equals(meter))
                .map(row -> cell(row, 4)).findFirst()
                .orElseThrow(() -> new AssertionError("no row " + meter));
    }

    private static String cell(TableRow row, int index) {
        return row.getDataCells().get(index).getElement()
                .getTextRecursively();
    }

    private static Component investigationOf(Component root) {
        return findById(root).orElseThrow(() -> new AssertionError(
                "no component with id 'investigation'"));
    }

    private static Optional<Component> findById(Component root) {
        if (root.getId().filter("investigation"::equals).isPresent()) {
            return Optional.of(root);
        }
        return root.getChildren().map(InteractionLatencyViewTest::findById)
                .flatMap(Optional::stream).findFirst();
    }
}
