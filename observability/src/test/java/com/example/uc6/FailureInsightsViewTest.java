package com.example.uc6;

import java.util.List;
import java.util.Optional;

import com.example.acme.AppWindow;
import com.example.home.HomeView;
import io.micrometer.core.instrument.Counter;
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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note on coverage: the kit captures an interaction from Flow's RPC invocation
 * listener, which only fires while handling a real UIDL request. A browserless
 * click invokes the listener directly, bypassing that pipeline, so these tests
 * verify the story, the reveal, the wiring and the lifecycle, but not the
 * capture itself — the populated verdict is covered by using the running
 * application. See the test-simulator note in {@code API-GAPS.md}.
 */
@SpringBootTest
@ViewPackages(classes = { FailureInsightsView.class, HomeView.class })
class FailureInsightsViewTest extends SpringBrowserlessTest {

    @Autowired
    MeterRegistry registry;

    @Test
    void opensWithTheReturnsDeskAndTheInvestigationHidden() {
        FailureInsightsView view = navigate(FailureInsightsView.class);

        assertEquals("UC6 — Why do some returns blow up?",
                findInView(H1.class).first().getText());
        assertNotNull(findInView(AppWindow.class).first(),
                "the Acme returns desk scene is what makes the readout a "
                        + "story");
        assertEquals("AC-10482",
                findInView(TextField.class).id("order-number").getValue(),
                "a plausible order number is prefilled so the first return "
                        + "is one click away");
        assertNotNull(findInView(Button.class).withText("Process return")
                .single());
        assertNotNull(findInView(Table.class).id("returns-log"));
        assertNotNull(findInView(Div.class).id("simulation-rig"),
                "the bank lookup latency is demo rigging, not a kit readout");
        assertFalse(investigationOf(view).isVisible(),
                "the investigation appears only once a return has blown up");
    }

    @Test
    void theNumberedRouteIsAnAliasForTheStoryRoute() {
        navigate(FailureInsightsView.class);

        assertEquals(FailureInsightsView.class,
                RouteConfiguration.forSessionScope().getRoute("uc6")
                        .orElseThrow(),
                "/uc6 must keep resolving to this view");
        assertEquals(FailureInsightsView.class,
                RouteConfiguration.forSessionScope()
                        .getRoute(FailureInsightsView.ROUTE).orElseThrow(),
                "/returns is the primary route the telemetry is tagged with");
    }

    @Test
    void aCleanReturnIsRegisteredWithoutRevealingTheInvestigation() {
        FailureInsightsView view = navigate(FailureInsightsView.class);

        processReturn();

        Table log = findInView(Table.class).id("returns-log");
        assertEquals(1, log.getBodyRows().size());
        assertEquals("AC-10482",
                log.getBodyRows().get(0).getDataCells().get(0).getText());
        assertFalse(investigationOf(view).isVisible(),
                "nothing went wrong, so there is nothing to investigate yet");
    }

    @Test
    void aDefectiveReturnBlowsUpAndRevealsTheInvestigation() {
        FailureInsightsView view = navigate(FailureInsightsView.class);
        selectReason(FailureInsightsView.REASON_DEFECTIVE);

        // The kit records a failed interaction only when the invocation
        // actually fails, so the handler must not swallow the exception.
        assertThrows(IllegalStateException.class, this::processReturn,
                "the failing handler must let its exception propagate");

        assertTrue(investigationOf(view).isVisible(),
                "the reveal happens before the work, so it survives the "
                        + "failure and lands in the same response");
        assertEquals(1, findInView(Table.class).id("returns-log")
                .getBodyRows().size(),
                "a failed return is not registered — only the empty-state "
                        + "row remains");
    }

    @Test
    void aBlankOrderNumberIsRejectedAndRevealsTheInvestigation() {
        FailureInsightsView view = navigate(FailureInsightsView.class);
        findInView(TextField.class).id("order-number").setValue("  ");

        assertThrows(IllegalArgumentException.class, this::processReturn);

        assertTrue(investigationOf(view).isVisible());
    }

    @Test
    void aBankTransferRefundGoesThroughSlowlyAndRevealsTheInvestigation() {
        FailureInsightsView view = navigate(FailureInsightsView.class);
        findInView(IntegerField.class).id("bank-delay").setValue(0);
        selectRefund(FailureInsightsView.REFUND_BANK_TRANSFER);

        processReturn();

        assertTrue(investigationOf(view).isVisible(),
                "a hang is a problem the clerk feels, so it reveals too");
        assertEquals(1, findInView(Table.class).id("returns-log")
                .getBodyRows().size(), "slow, but registered");
    }

    @Test
    void theBankLookupDefaultsAboveTheInsightsBudget() {
        navigate(FailureInsightsView.class);

        assertTrue(findInView(IntegerField.class).id("bank-delay")
                .getValue() > 1_000,
                "the first bank-transfer refund must already exceed the 1 s "
                        + "UX budget, or step 3 shows nothing for it");
    }

    @Test
    void walksTheInvestigationOneCollapsibleStepAtATime() {
        navigate(FailureInsightsView.class);
        selectReason(FailureInsightsView.REASON_DEFECTIVE);
        assertThrows(IllegalStateException.class, this::processReturn);

        assertEquals(List.of("1 — Process a few returns"),
                findInView(H3.class).all().stream().map(H3::getText).toList());
        List<Details> steps = findInView(Details.class).all();
        assertEquals(List.of("2 — The usual suspects know that, not where",
                "3 — The kit's verdict", "4 — The payload an agent reads"),
                steps.stream().map(Details::getSummaryText).toList());
        assertTrue(steps.get(0).isOpened());
        assertFalse(steps.get(1).isOpened());
        assertFalse(steps.get(2).isOpened());
        assertNotNull(findInView(Paragraph.class).id("error-counter"));
        assertTrue(findInView(Grid.class).all().isEmpty(),
                "the readout uses no Grid, so it records no data queries on "
                        + "the route whose failures it explains");
    }

    @Test
    void theErrorCounterStepReadsOnlyThisRoute() {
        navigate(FailureInsightsView.class);
        // What the kit's ErrorCounter records for the defective-item failure
        // on this route, and what some other view's failures look like.
        errors("java.lang.IllegalStateException", FailureInsightsView.ROUTE,
                "com.vaadin.flow.component.button.Button").increment(3);
        errors("java.lang.IllegalArgumentException", "inventory",
                "com.vaadin.flow.component.button.Button").increment(999);

        selectReason(FailureInsightsView.REASON_DEFECTIVE);
        assertThrows(IllegalStateException.class, this::processReturn);

        String text = findInView(Paragraph.class).id("error-counter")
                .getElement().getTextRecursively();
        assertTrue(text.contains("vaadin.errors"), text);
        assertTrue(text.contains("IllegalStateException on Button"), text);
        assertTrue(text.contains("×3"), text);
        assertFalse(text.contains("999"),
                "another route's failures must not leak into this desk's "
                        + "readout");
    }

    @Test
    void theVerdictExplainsItselfWhileEmpty() {
        navigate(FailureInsightsView.class);
        selectReason(FailureInsightsView.REASON_DEFECTIVE);
        assertThrows(IllegalStateException.class, this::processReturn);
        findInView(Details.class).all().forEach(d -> d.setOpened(true));

        Div verdict = findInView(Div.class).id("verdict");
        assertTrue(verdict.getElement().getTextRecursively()
                .contains("UX budget"),
                "the empty verdict must say what would make a finding appear");
    }

    @Test
    void doesNotPollAndLeavesTheSessionErrorHandlerAlone() {
        ErrorHandler original = VaadinSession.getCurrent().getErrorHandler();

        navigate(FailureInsightsView.class);
        assertEquals(-1, UI.getCurrent().getPollInterval(),
                "the readout refreshes per interaction, so it must not poll");
        assertSame(original, VaadinSession.getCurrent().getErrorHandler(),
                "the view shows its notification from the handler itself; it "
                        + "installs nothing on the session, so there is "
                        + "nothing to restore or leak");
        selectReason(FailureInsightsView.REASON_DEFECTIVE);
        assertThrows(IllegalStateException.class, this::processReturn);
        assertTrue($(Notification.class).all().stream()
                .anyMatch(Notification::isOpened),
                "the clerk sees a notification even though the exception "
                        + "propagates");

        navigate(HomeView.class);
        assertSame(original, VaadinSession.getCurrent().getErrorHandler());
    }

    @Test
    void readoutIsApplicationScopedAcrossSessions() {
        // The insights come from an application-scoped buffer the kit owns,
        // not from per-session state: a second session must see the same
        // readout.
        navigate(FailureInsightsView.class);
        selectReason(FailureInsightsView.REASON_DEFECTIVE);
        assertThrows(IllegalStateException.class, this::processReturn);
        long firstSessionCards = findInView(Div.class).id("verdict")
                .getChildren().count();

        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(FailureInsightsView.class);
        selectReason(FailureInsightsView.REASON_DEFECTIVE);
        assertThrows(IllegalStateException.class, this::processReturn);

        assertEquals(firstSessionCards, findInView(Div.class).id("verdict")
                .getChildren().count(),
                "both sessions should read the same application-wide insights");
    }

    private void processReturn() {
        test(findInView(Button.class).withText("Process return").single())
                .click();
    }

    private void selectReason(String reason) {
        test(findInView(Select.class).id("return-reason"), String.class)
                .selectItem(reason);
    }

    private void selectRefund(String refund) {
        test(findInView(Select.class).id("refund-method"), String.class)
                .selectItem(refund);
    }

    private Counter errors(String exception, String route, String component) {
        return Counter.builder("vaadin.errors").tag("exception", exception)
                .tag("route", route).tag("component", component)
                .register(registry);
    }

    private static Component investigationOf(Component root) {
        return findById(root).orElseThrow(() -> new AssertionError(
                "no component with id 'investigation'"));
    }

    private static Optional<Component> findById(Component root) {
        if (root.getId().filter("investigation"::equals).isPresent()) {
            return Optional.of(root);
        }
        return root.getChildren().map(FailureInsightsViewTest::findById)
                .flatMap(Optional::stream).findFirst();
    }
}
