package com.example.uc2;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.example.acme.AppWindow;
import com.example.home.HomeView;
import com.example.uc2.ApplicationHealthView.Channel;
import com.example.uc2.ProductCatalogService.CatalogLoad;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { ApplicationHealthView.class, HomeView.class })
class ApplicationHealthViewTest extends SpringBrowserlessTest {

    @Autowired
    MeterRegistry registry;

    @Autowired
    ProductCatalogService catalog;

    @Test
    void opensWithTheInventoryPageAndTheInvestigationHidden() {
        ApplicationHealthView view = navigate(ApplicationHealthView.class);

        assertTrue(findInView(H1.class).first().getText().startsWith("UC2"));
        assertNotNull(findInView(AppWindow.class).first(),
                "the Acme inventory page is what makes the readout a story");
        assertNotNull(findInView(Button.class).id("load-catalog"));
        assertNotNull(findInView(Table.class).id("catalog"));
        assertNotNull(findInView(Checkbox.class).id("join-fetch"),
                "the fix is a demo-rig switch, not a kit readout");
        assertNotNull(findInView(Button.class).id("flush-client"),
                "flushing the client collector is demo rigging too");
        assertFalse(investigationOf(view).isVisible(),
                "the investigation appears only once the catalog has been "
                        + "loaded and the hiccup felt");
    }

    @Test
    void theNumberedRouteIsAnAliasForTheStoryRoute() {
        navigate(ApplicationHealthView.class);

        assertEquals(ApplicationHealthView.class,
                RouteConfiguration.forSessionScope().getRoute("uc2")
                        .orElseThrow());
        assertEquals(ApplicationHealthView.class,
                RouteConfiguration.forSessionScope()
                        .getRoute(ApplicationHealthView.ROUTE).orElseThrow(),
                "/inventory is the primary route the fetch summary is tagged "
                        + "with");
    }

    @Test
    void loadingTheCatalogFillsThePageAndRevealsTheInvestigation() {
        ApplicationHealthView view = navigate(ApplicationHealthView.class);

        loadCatalog();

        Table table = findInView(Table.class).id("catalog");
        assertTrue(table.getBodyRows().size() > 1,
                "the inventory page lists the products it fetched");
        assertTrue(table.getElement().getTextRecursively().contains("bolt"),
                "Acme sells fasteners, not programming books");
        assertTrue(investigationOf(view).isVisible());
    }

    @Test
    void walksTheInvestigationOneCollapsibleStepAtATime() {
        navigate(ApplicationHealthView.class);
        loadCatalog();

        assertEquals(List.of("1 — Open the catalog"),
                findInView(H3.class).all().stream().map(H3::getText).toList());
        List<Details> steps = findInView(Details.class).all();
        assertEquals(List.of("2 — The vital signs look fine",
                "3 — The database gives it away", "4 — The fix, verified"),
                steps.stream().map(Details::getSummaryText).toList());
        assertTrue(steps.get(0).isOpened());
        assertFalse(steps.get(1).isOpened());
        assertFalse(steps.get(2).isOpened());
        assertTrue(findInView(Grid.class).all().isEmpty(),
                "the readout uses no Grid: a Grid would record data queries "
                        + "on the very route whose database cost it explains");
    }

    @Test
    void theKitFetchMeterRevealsTheNPlusOne() {
        navigate(ApplicationHealthView.class);
        loadCatalog();
        openAllSteps();

        // The module pins a kit build with the database feature and sets
        // vaadin.observability.database=true, so the meter must be there,
        // tagged with this route. Asserted rather than assumed: if the kit ever
        // stopped recording it, UC2's headline claim would be gone.
        assertFalse(registry.find(ApplicationHealthView.DB_FETCH_ROWS)
                .tag("route", ApplicationHealthView.ROUTE).summaries()
                .isEmpty(),
                "the kit should record vaadin.db.fetch.rows for route="
                        + ApplicationHealthView.ROUTE);

        String text = findInView(Paragraph.class).id("catalog-result")
                .getElement().getTextRecursively();
        assertTrue(text.contains("vaadin.db.fetch.rows"), text);
        assertTrue(text.contains("N+1"),
                "the eager, unbatched join fetch must be flagged as N+1: "
                        + text);

        List<TableRow> history = findInView(Table.class)
                .id("load-history").getBodyRows();
        assertEquals(1, history.size());
        assertTrue(cell(history.get(0), 0).contains("eager, unbatched"));
        assertTrue(Long.parseLong(cell(history.get(0), 2)) > Long
                .parseLong(cell(history.get(0), 1)),
                "more fetches than products is the N+1 signature");
    }

    @Test
    void theJoinFetchSwitchCollapsesTheNPlusOne() {
        navigate(ApplicationHealthView.class);
        loadCatalog();
        findInView(Checkbox.class).id("join-fetch").setValue(true);
        loadCatalog();
        openAllSteps();

        List<TableRow> history = findInView(Table.class)
                .id("load-history").getBodyRows();
        assertEquals(2, history.size(), "both loads are kept for the before/after");
        TableRow fixed = history.get(1);
        assertTrue(cell(fixed, 0).contains("join fetch"));
        assertTrue(Long.parseLong(cell(fixed, 2)) <= 2,
                "with the join fetch the categories come along in the product "
                        + "query, so the fetch count collapses");
        String text = findInView(Paragraph.class).id("catalog-result")
                .getElement().getTextRecursively();
        assertTrue(text.contains("No N+1"), text);
    }

    @Test
    void theVitalSignsReadTheSharedRegistryAcrossSessions() {
        // Session 1 records an interaction into the application-scoped
        // registry the view reads from.
        navigate(ApplicationHealthView.class);
        registry.timer("vaadin.request.duration", "outcome", "success")
                .record(Duration.ofMillis(42));

        // Session 2: a fresh Vaadin environment, same Spring context. The
        // timing recorded by session 1 must be visible — the readout reflects
        // shared, app-wide state rather than per-view bookkeeping.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(ApplicationHealthView.class);
        loadCatalog();

        TableRow requests = findInView(Table.class)
                .id("vital-signs").getBodyRows().stream()
                .filter(row -> "vaadin.request.duration"
                        .equals(cell(row, 0)))
                .findFirst().orElseThrow();
        assertFalse("—".equals(cell(requests, 3)),
                "an interaction recorded in the first session should be "
                        + "visible in the second");
        assertTrue(findInView(Table.class).id("vital-signs").getBody()
                .getRows().stream().anyMatch(row -> "vaadin.db.fetch.rows"
                        .equals(cell(row, 0))),
                "the kit's database meter is one of the vital signs");
    }

    @Test
    void pollingDrivesTheLiveReadout() {
        navigate(ApplicationHealthView.class);
        loadCatalog();

        Span status = findInView(Span.class).id("connection-status");
        String first = status.getText();

        // A poll tick recomputes the readout; the refresh counter in the
        // connection line must advance. The view registers its refresh as a UI
        // poll listener in onAttach, so firing a PollEvent drives that path.
        UI ui = UI.getCurrent();
        ComponentUtil.fireEvent(ui, new PollEvent(ui, false));

        String second = status.getText();
        assertFalse(first.equals(second),
                "a poll tick should refresh the live status badge");
        assertTrue(second.startsWith("Live"),
                "an on-time tick should report a live channel: " + second);
        assertTrue(status.getElement().getThemeList().contains("success"));
        assertFalse(status.getElement().getThemeList().contains("error"));
    }

    @Test
    void channelStateIsDerivedFromTheRefreshCadence() {
        int poll = 2000;
        assertEquals(Channel.UNKNOWN,
                ApplicationHealthView.channel(0, 10_000, poll),
                "before the first tick the channel state is unknown");
        assertEquals(Channel.LIVE,
                ApplicationHealthView.channel(10_000, 12_000, poll),
                "a tick within the poll window means updates are flowing");
        assertEquals(Channel.LIVE,
                ApplicationHealthView.channel(10_000, 14_000, poll),
                "a tick at twice the poll interval is still on time");
        assertEquals(Channel.RESUMED,
                ApplicationHealthView.channel(10_000, 25_000, poll),
                "a late tick means updates had stopped arriving");
    }

    @Test
    void pollingStopsAfterLeavingTheView() {
        navigate(ApplicationHealthView.class);
        assertTrue(UI.getCurrent().getPollInterval() > 0,
                "the view enables polling while attached");

        navigate(HomeView.class);
        assertEquals(-1, UI.getCurrent().getPollInterval(),
                "polling is disabled again once the view is detached");
    }

    @Test
    void flushingClientMetricsIsSafeWithoutABrowser() {
        // The flush asks the browser collector to POST its buffer via
        // executeJs; with no browser the click must be a no-op that leaves the
        // view intact rather than throwing.
        navigate(ApplicationHealthView.class);
        loadCatalog();

        test(findInView(Button.class).id("flush-client")).click();

        assertTrue(findInView(Span.class).id("connection-status").getText()
                .contains("refreshes this session"));
    }

    @Test
    void catalogLoadIsSeededAndReadable() {
        // Service-level smoke test of the JPA layer and the seeder: the
        // catalog must come back non-empty so the demo has something to fan
        // out over, and the join-fetching load must return the same catalog.
        CatalogLoad plain = catalog.loadCatalog(false);
        CatalogLoad joined = catalog.loadCatalog(true);
        assertTrue(plain.products() > 0, "the catalog should be seeded");
        assertTrue(plain.categories() >= plain.products(),
                "every product should carry at least one category link");
        assertEquals(plain.lines(), joined.lines(),
                "the fix changes how the catalog is fetched, not what it is");
    }

    private void loadCatalog() {
        test(findInView(Button.class).id("load-catalog")).click();
    }

    private void openAllSteps() {
        findInView(Details.class).all()
                .forEach(step -> step.setOpened(true));
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
        return root.getChildren().map(ApplicationHealthViewTest::findById)
                .flatMap(Optional::stream).findFirst();
    }
}
